(ns leiningen.lein-sassy.renderer
  (:require
    [clojure.java.io :as io]
    [clojure.string :as s]
    [hawk.core :as hawk]
    [leiningen.lein-sassy.file-utils :refer :all]
    [leiningen.lein-sassy.ruby :refer :all]
    [me.raynes.fs :as fs])
  (:import
    (java.util Base64 Date)))

(defn- print-message [& args]
  (println (apply str (into [] (concat [(str "[" (Date.) "] ")] args)))))

(defn- init-gems
  "Imports and loads the needed gems."
  [container options]
  (add-gemjars)
  (require-gem container "sass")
  (require-gem container "autoprefixer-rails"))

(defn init-renderer
  "Creates a container and runtime for the renderer to use."
  [options]
  (let [container (make-container)
        runtime (make-runtime container)]
    (do (init-gems container options)
        {:container container :runtime runtime})))

(defn- apply-autoprefixer [container runtime options outpath [css map]]
  (let [sourcemap-type (-> options :sourcemap #{:auto :inline})
        autoprefixer-options {:from (fs/base-name outpath)
                              :map (if-not sourcemap-type false {:inline false})}
        args (to-array [css (make-rb-hash runtime autoprefixer-options)])
        autoprefixer (run-ruby container "AutoprefixerRails")
        prefixed (call-ruby-method container autoprefixer "process" args Object)]
    [(str prefixed) map]))

(defn- plugins-pipe [container runtime options outpath rendered]
  (reduce
    #(case %2
       :autoprefixer (apply-autoprefixer container runtime options outpath %1)
       %1)
    rendered
    (:plugins options)))

(defn- render-with-sourcemap [container runtime engine options outpath sourcemap-options]
  (let [map-uri (fs/base-name (:sourcemap_path sourcemap-options))
        rendered (call-ruby-method container engine "render_with_sourcemap" map-uri Object)
        [css sourcemap] (plugins-pipe container runtime options outpath rendered)
        json-options (make-rb-hash runtime (merge sourcemap-options {:type :auto})) ;; override :auto here to force relative path
        map-json (call-ruby-method container sourcemap "to_json" json-options String)]
    (if (= :inline (:type sourcemap-options))
      [(s/replace css (str "sourceMappingURL=" map-uri)
                      (str "sourceMappingURL=data:application/json;base64,"
                        (.encodeToString (Base64/getEncoder) (.getBytes map-json))))
       nil]
      [css map-json])))

(defn- render [container runtime engine options outpath]
  (plugins-pipe container runtime options outpath
    [(call-ruby-method container engine "render" String)
     nil]))

(defn render-file
  "Renders one file and returns the rendered result and the sourcemap."
  [container runtime options file outpath]
  (let [syntax (get-file-syntax file options)
        options (merge options {:syntax syntax :filename (str file)})
        sass-options (make-rb-hash runtime (select-keys options [:syntax :style :load_paths :filename]))
        args (to-array [(slurp file) sass-options])
        sass (run-ruby container "Sass::Engine")
        engine (call-ruby-method container sass "new" args Object)]
    (try (if-let [type (-> options :sourcemap #{:auto :inline})]
           (let [map-options {:type type :css_path outpath :sourcemap_path (map-path outpath)}]
             (render-with-sourcemap container runtime engine options outpath map-options))
           (render container runtime engine options outpath))
      (catch Exception e (print-message "Compilation failed:" e)))))

(defn- spit-files! [css sourcemap outpath map-type]
  (fs/mkdirs (fs/parent outpath))
  (spit outpath css)
  (when (and sourcemap (not (= :inline map-type)))
    (spit (map-path outpath) sourcemap)))

(defn render-all!
  "Renders all templates in the directory specified by (:src options)."
  [container runtime options]
  (doseq [file (fs/find-files* (:src options) compilable-sass-file?)]
    (let [inpath (fs/normalized file)
          outpath (dest-path (:src options) file (:dst options))
          rel-file (relative-path file fs/*cwd*)
          rel-outpath (relative-path outpath fs/*cwd*)
          [css sourcemap] (render-file container runtime options rel-file rel-outpath)]
      (print-message inpath " to " outpath)
      (spit-files! css sourcemap outpath (:sourcemap options)))))

(defn- file-change-handler
  "Prints the file that was changed then renders all templates."
  [container runtime options file]
  (when (sass-file? file)
    (print-message "File " file " changed.")
    (render-all! container runtime options)))

(defn watch-and-render!
  "Watches the directory specified by (:src options) and calls a handler that
  renders all templates."
  [container runtime options]
  (print-message "Watching " (:src options) " for changes.")
  (print-message "Type \"exit\" to stop.")
  (let [handler (partial file-change-handler container runtime options)
        watcher (hawk/watch! [{:paths [(:src options)]
                               :handler (fn [_ {:keys [kind file]}]
                                          (when (= :modify kind)
                                            (handler (str file))))}])]
    (loop []
      (if (= (read-line) "exit")
        (hawk/stop! watcher)
        (recur)))))
