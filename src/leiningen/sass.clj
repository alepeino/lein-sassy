(ns leiningen.sass
  (:require
    [leiningen.lein-sassy.file-utils :refer :all]
    [leiningen.lein-sassy.renderer :refer :all]
    [leiningen.help :as lhelp]
    [leiningen.core.main :as lmain]
    [me.raynes.fs :as fs]
    [zweikopf.core :as z]))

(def ^:private default-options {:src "resources/public/stylesheets"
                                :dst "resources/app/stylesheets"
                                :delete-output-dir true
                                :style :nested})

(defn get-sass-options [project]
  (if-let [options (:sass project)]
    (merge
      default-options
      {:load_paths (map str (fs/find-files* (:src options) fs/directory?))}
      options)
    (lmain/warn "No sass entry found in project definition.")))

(defn- init-ruby-context [options]
  (z/init-ruby-context)
  (z/set-gem-path "target/rubygems-provided")
  (z/ruby-require "sass"))

(defn once
  "Compile files in :src location and exit."
  [options]
  (init-ruby-context options)
  (render-all! options))

(defn watch
  "Automatically recompile when files are modified."
  [options]
  (init-ruby-context options)
  (render-all! options)
  (watch-and-render! options))

(defn clean
  "Clean files compiled by lein-sass in :dst location."
  [options]
  (init-ruby-context options)
  (clean-all! options))

(defn sass
  {:help-arglists '[[once] [watch] [clean]]
   :subtasks [#'once #'watch #'clean]
   :doc "Compile Sass files."}

  ([project]
   (lmain/abort (lhelp/help-for "sass")))

  ([project subtask & args]
   (if-let [options (get-sass-options project)]
     (case subtask
       "once" (once options)
       (or "auto" "watch") (watch options)
       "clean" (clean options)
       (lmain/warn subtask " not found."))
     (lmain/abort "Invalid options in project.clj."))))
