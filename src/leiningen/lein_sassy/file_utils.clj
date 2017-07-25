(ns leiningen.lein-sassy.file-utils
  (:require
    [clojure.string :as s]
    [leiningen.core.main :as lmain]
    [me.raynes.fs :as fs]))

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
  (-> file fs/base-name first #{\_} boolean))

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
  "Returns relative path from file to parent directory, or the path unchanged
  if parent is not a parent directory of path."
  [path parent]
  (let [abs-path (str (fs/normalized path))
        abs-parent (str (fs/normalized parent))]
    (if (s/starts-with? abs-path abs-parent)
      (s/replace-first abs-path (str abs-parent java.io.File/separator) "")
      path)))

(defn dest-path
  "Gets the destination path for a file to be rendered ('css' extension and
  same relative path to base directories)."
  [src-file src-dir dest-dir]
  (let [insubpath (relative-path src-file src-dir)
        outsubpath (filename-to-css insubpath)]
    (str (fs/file dest-dir (filename-to-css outsubpath)))))

(defn map-file
  "Gets the path for the map file given a css file."
  [css-file]
  (str css-file map-extension))

(defn delete-file!
  "Deletes a single file if exists."
  [file]
  (when (fs/exists? file)
    (lmain/info (str "Deleting: " file))
    (fs/delete file)))

(defn delete-dir!
  "Deletes directory recursively."
  [base-dir]
  (when (and (fs/exists? base-dir) (fs/directory? base-dir))
    (lmain/info (str "Deleting directory: " base-dir))
    (fs/delete-dir base-dir)))
