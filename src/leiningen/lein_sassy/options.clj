(ns leiningen.lein-sassy.options
  (:require [leiningen.core.main :as lmain]))

(def ^:private default-options {:src "resources/public/stylesheets"
                                :dst "resources/app/stylesheets"
                                :delete-output-dir true
                                :style :nested})

(defn get-sass-options [project]
  (if-let [options (:sass project)]
    (merge
      default-options
      (let [directory (clojure.java.io/file (:src options))
            directories (filter #(.isDirectory %) (file-seq directory))
            load-paths (into #{(:src options)} (map #(.getPath %) directories))]
        (merge options {:load_paths (vec load-paths)}))
      options)
    (lmain/warn "No sass entry found in project definition.")))
