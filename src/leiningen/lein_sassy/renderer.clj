(ns leiningen.lein-sassy.renderer
  (:require [leiningen.lein-sassy.ruby :refer :all]
            [clojure.string :as s]
            [panoptic.core :refer :all]
            [clojure.java.io :as io]))

(def ^:private sass-extensions ["sass" "scss"])
(def ^:private watch-poll-rate 50)

(defn- get-file-extension [file]
  (when file
    (let [filename (.getPath file)
          dot (.lastIndexOf filename ".")]
      (when (pos? dot)
        (subs filename (inc dot))))))

(defn- get-syntax
  "Gets the syntax given a file and options hash. If the hash defines the
  syntax, return that. Otherwise, return the file's extension."
  [file options]
  (or (:syntax options)
      (get-file-extension file)))

(defn- init-gems
  "Installs and loads the needed gems."
  [container options]
  (do (install-gem (:gem-name options) (:gem-version options))
      (require-gem container (str (:gem-name options) "/util"))
      (require-gem container (str (:gem-name options) "/engine"))
      (require-gem container (:gem-name options))))

(defn init-renderer
  "Creates a container and runtime for the renderer to use."
  [options]
  (let [container (make-container)
        runtime (make-runtime container)]
    (do (init-gems container options)
        {:container container :runtime runtime})))

(defn render
  "Renders one template and returns the result."
  [container runtime options template]
  (let [sass-options (make-rb-hash runtime (select-keys options [:syntax :style]))
        args (to-array [template sass-options])
        sass (run-ruby container "Sass::Engine")
        engine (call-ruby-method container sass "new" args Object)]
    (try (call-ruby-method container engine "render" String)
         (catch Exception e (println "Compilation failed:" e)))))

(defn render-all!
  "Renders all templates in the directory specified by (:src options)."
  [container runtime options]
  (let [directory (clojure.java.io/file (:src options))
        files (remove #(.isDirectory %) (file-seq directory))]
    (doseq [file files]
      (let [subpath (s/replace-first (.getPath file) (:src options) "")
            subpath (s/replace subpath (re-pattern (str ".(" (s/join "|" sass-extensions) ")$")) ".css")
            outpath (str (:dst options) subpath)
            syntax (get-syntax file options)
            rendered (render container runtime (merge options {:syntax syntax}) (slurp file))]
        (if-not (.exists (io/file (.getParent (io/file outpath)))) (io/make-parents outpath))
        (spit outpath rendered)))))

(defn- file-change-handler
  "Prints the file that was changed then renders all templates."
  [container runtime options _1 _2 file]
  (do (println "File" (:path file) "changed.")
      (render-all! container runtime options)))

(defn watch-and-render!
  "Watches the directory specified by (:src options) and calls a handler that
  renders all templates."
  [container runtime options]
  (println "Watching" (:src options) "for changes.")
  (let [handler (partial file-change-handler container runtime options)
        fw (->  (file-watcher)
                (on-file-create handler)
                (on-file-modify handler)
                (unwatch-on-delete)
                (run!))
        dw (->  (directory-watcher :recursive true)
                (on-directory-create (fn [_1 _2 dir]
                  (doseq [child (:files (:panoptic.data.core/children dir))]
                    (watch-entity! fw (str (:path dir) "/" child) :created))))
                 (on-file-create #(watch-entity! fw (:path %3) :created))
                 (run!))]
    (watch-entity! dw (:src options) :created)
   @dw))
