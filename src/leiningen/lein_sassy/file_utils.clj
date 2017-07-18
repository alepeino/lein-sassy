(ns leiningen.lein-sassy.file-utils
  (:require
    [clojure.string :as s]
    [me.raynes.fs :as fs]
    [clojure.java.io :as io]))

(def sass-extensions #{"sass" "scss"})
(def css-extension "css")

(defn file-extension
  [file]
  (some-> (.getPath file) fs/extension (subs 1)))

(defn sass-file?
  "Returns whether or not a file ends in a sass extension."
  [file]
  (contains? sass-extensions (file-extension file)))

(defn sass-partial?
  "Returns whether or not a file is a partial (i.e. starts with an
  underscore)."
  [file]
  (.startsWith (.getName file) "_"))

(defn compilable-sass-file?
  "Returns whether or not a file is a sass file that can be compiled (i.e.
  not a partial)."
  [file]
  (and (.isFile file)
       (sass-file? file)
       (not (sass-partial? file))))

(defn get-file-syntax
  "Gets the syntax given a file and options hash. If the hash defines the
  syntax, return that. Otherwise, return the file's extension."
  [file options]
  (or (:syntax options)
      (keyword (file-extension file))))

(defn filename-to-css
  "Changes a file's extension to the css extension."
  [filename]
  (s/replace
    filename
    (->> sass-extensions (map #(str % "$")) (s/join "|") (re-pattern))
    css-extension))

(defn- dest-file
  [src-file src-dir dest-dir]
  (let [src-dir (.getCanonicalPath (io/file src-dir))
        dest-dir (.getCanonicalPath (io/file dest-dir))
        src-path (.getCanonicalPath src-file)
        src-base-name (fs/base-name src-path)
        rel-dest-path (filename-to-css src-base-name)]
    (io/file (str dest-dir rel-dest-path))))

(defn sass-css-mapping
  [{:keys [src dst]}]
  (let [sass-files (fs/find-files* src sass-file?)]
    (reduce
     (fn [sass-mapping sass-file]
       (assoc sass-mapping sass-file (dest-file sass-file src dst)))
     {} sass-files)))

(defn dir-empty?
  [dir]
  (not-any? #(.isFile %)
            (file-seq (io/file dir))))

(defn delete-file!
  [file]
  (when (fs/exists? file)
    (println (str "Deleting: " file))
    (fs/delete file)))

(defn delete-dir!
  "Deletes directory recursively"
  [base-dir]
  (when (and (fs/exists? base-dir) (fs/directory? base-dir))
    (println (str "Deleting directory: " base-dir))
    (fs/delete-dir base-dir)))

(defn map-file
  [file]
  (str file ".map"))
