(ns leiningen.lein-sassy.renderer
  (:refer-clojure :exclude [run!])
  (:require
    [clojure.java.io :as io]
    [clojure.string :as s]
    [leiningen.lein-sassy.file-utils :refer :all]
    [leiningen.lein-sassy.ruby :refer :all]
    [me.raynes.fs :as fs]
    [panoptic.core :refer :all])
  (:import
    (java.util Base64 Date)))

(defn- print-message [& args]
  (println (apply str (into [] (concat [(str "[" (Date.) "] ")] args)))))

(defn- init-gems
  "Imports and loads the needed gems."
  [container options]
  (add-gemjars)
  (require-gem container "sass"))

(defn init-renderer
  "Creates a container and runtime for the renderer to use."
  [options]
  (let [container (make-container)
        runtime (make-runtime container)]
    (do (init-gems container options)
        {:container container :runtime runtime})))

(defn- render-with-sourcemap [container runtime engine sourcemap-options]
  (let [map-uri (fs/base-name (:sourcemap_path sourcemap-options))
        [rendered sourcemap] (call-ruby-method container engine "render_with_sourcemap" map-uri Object)
        json-options (make-rb-hash runtime (merge sourcemap-options {:type :auto})) ;; override :auto here to force relative path
        map-json (call-ruby-method container sourcemap "to_json" json-options String)]
    (if (= :inline (:type sourcemap-options))
      [(s/replace rendered (str "sourceMappingURL=" map-uri)
                           (str "sourceMappingURL=data:application/json;base64,"
                             (.encodeToString (Base64/getEncoder) (.getBytes map-json))))
       nil]
      [rendered map-json])))

(defn- render [container runtime engine]
  [(call-ruby-method container engine "render" String) nil])

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
             (render-with-sourcemap container runtime engine map-options))
           (render container runtime engine))
         (catch Exception e (print-message "Compilation failed:" e)))))

(defn- spit-files! [rendered sourcemap outpath map-type]
  (fs/mkdirs (fs/parent outpath))
  (spit outpath rendered)
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
          [rendered sourcemap] (render-file container runtime options rel-file rel-outpath)]
      (print-message inpath " to " outpath)
      (spit-files! rendered sourcemap outpath (:sourcemap options)))))

(defn- file-change-handler
  "Prints the file that was changed then renders all templates."
  [container runtime options _1 _2 file]
  (do (print-message "File " (:path file) " changed.")
      (render-all! container runtime options)))

(defn watch-and-render!
  "Watches the directory specified by (:src options) and calls a handler that
  renders all templates."
  [container runtime options]
  (print-message "Watching " (:src options) " for changes.")
  (let [handler (partial file-change-handler container runtime options)
        fw (->  (file-watcher)
                (on-file-create handler)
                (on-file-modify handler)
                (unwatch-on-delete)
                (run!))
        dw (->  (directory-watcher :recursive true)
                (on-directory-create (fn [_ _ dir]
                                       (doseq [child (:files (:panoptic.data.core/children dir))]
                                         (watch-entity! fw (str (:path dir) "/" child) :created))))
                (on-file-create #(watch-entity! fw (:path %3) :created))
                (run!))]
    (watch-entity! dw (:src options) :created)
   @dw))
