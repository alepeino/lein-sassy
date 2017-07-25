(ns leiningen.lein-sassy.renderer
  (:require
    [clojure.string :as s]
    [hawk.core :as hawk]
    [leiningen.core.main :as lmain]
    [leiningen.lein-sassy.file-utils :refer :all]
    [me.raynes.fs :as fs]
    [zweikopf.core :as z]))

(defn- print-message [& args]
  (lmain/info (apply str (.format (java.text.SimpleDateFormat. "[yyyy-MM-dd HH:mm:ss] ") (java.util.Date.)) args)))

(defn render
  "Renders one template and returns the result."
  [options template]
  (try
    (let [sass-options (z/rubyize (select-keys  options [:syntax :style :load_paths :cache]))
          engine (z/call-ruby "Sass::Engine" "new" template sass-options)]
      (z/call-ruby engine "render"))
    (catch Exception e (print-message "Compilation failed:" e))))

(defn render-all!
  "Renders all templates in the directory specified by (:src options)."
  [options]
  (doseq [file (fs/find-files* (:src options) compilable-sass-file?)]
    (let [syntax (get-file-syntax file options)
          options (merge options {:syntax syntax})
          inpath (str file)
          outpath (dest-path inpath (:src options) (:dst options))
          rendered (render options (slurp file))]
      (print-message inpath " to " outpath)
      (fs/mkdirs (fs/parent outpath))
      (spit outpath rendered))))

(defn- file-change-handler
  "Prints the file that was changed then renders all templates."
  [options file]
  (when (sass-file? file)
    (print-message "File " file " changed.")
    (render-all! options)))

(defn watch-and-render!
  "Watches the directory specified by (:src options) and calls a handler that
  renders all templates."
  [options]
  (print-message "Watching " (:src options) " for changes.")
  (print-message "Type \"exit\" to stop.")
  (let [watcher (hawk/watch! [{:paths [(:src options)]
                               :handler (fn [_ {:keys [kind file]}]
                                          (when (= :modify kind)
                                            (file-change-handler options file)))}])]
    (while (not= (read-line) "exit"))
    (hawk/stop! watcher)))

(defn clean-all!
  [{:keys [dst delete-output-dir]}]
  (lmain/info "Deleting files generated by lein-sass in" dst)
  (if delete-output-dir
    (delete-dir! dst)
    (doseq [file (fs/find-files* dst (comp #{css-extension} fs/extension))]
      (delete-file! file)
      (delete-file! (map-file file)))))
