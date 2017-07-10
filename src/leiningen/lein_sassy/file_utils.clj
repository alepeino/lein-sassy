(ns leiningen.lein-sassy.file-utils
  (:require
    [clojure.string :as s]
    [me.raynes.fs :as fs])
  (:import
    (java.io File)))

(def sass-extensions #{".sass" ".scss"})
(def css-extension ".css")
(def map-extension ".map")

(defn sass-file?
  "Returns whether or not a file ends in a sass extension."
  [file]
  (contains? sass-extensions (fs/extension file)))

(defn sass-partial?
  "Returns whether or not a file is a partial (i.e. starts with an
  underscore)."
  [file]
  (s/starts-with? (fs/base-name file) "_"))

(defn compilable-sass-file?
  "Returns whether or not a file is a sass file that can be compiled (i.e.
  not a partial)."
  [file]
  (and (fs/file? file)
       (sass-file? file)
       (not (sass-partial? file))))

(defn get-file-syntax
  "Gets the syntax given a file and options hash. If the hash defines the
  syntax, return that. Otherwise, return a keyword with the file's extension."
  [file options]
  (or (:syntax options)
      (-> file (fs/extension) (subs 1) (keyword))))

(defn filename-to-css
  "Changes a file's extension to the css extension."
  [filename]
  (s/replace
    filename
    (->> sass-extensions (map #(str % "$")) (s/join "|") (re-pattern))
    css-extension))

(defn relative-path
  "Returns relative path from file to parent-dir directory, or nil if the file is not relative to parent."
  [file parent-dir]
  (let [dir-path (str parent-dir File/separator)]
    (when (s/starts-with? file dir-path)
          (s/replace-first file dir-path ""))))

(defn dest-path
  "Gets destination path for a file to be compiled."
  [src-dir src-file dest-dir]
  (let [src-dir (fs/file src-dir)
        src-file (fs/file src-file)
        src-rel-path (or (relative-path src-file src-dir)
                         (fs/normalized src-file))
        dest-rel-path (filename-to-css src-rel-path)]
    (str (fs/normalized (fs/file dest-dir dest-rel-path)))))

(defn map-path
  "Gets relative path for a map file given a css file"
  [css-file]
  (str css-file map-extension))

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

(defn clean-all!
  [{:keys [dst delete-output-dir]}]
  (if delete-output-dir
    (delete-dir! dst)
    (doseq [file (fs/find-files* dst (comp #{css-extension} fs/extension))]
      (delete-file! file)
      (delete-file! (map-path file)))))
