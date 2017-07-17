(ns leiningen.lein-sassy.gem-install
  (:require
    [clojure.java.shell :refer [sh]]
    [me.raynes.fs :as fs]))

(def gems-dir "resources/gemjars")

(defn gem-install [& gem]
  (apply println "Installing gem" gem)
  (.run (org.jruby.Main.)
    (into-array java.lang.String
      (concat ["-S" "gem" "install"
               "--source" "http://rubygems.org"
               "-i" gems-dir]
              gem))))

(defn build-jar []
  (println "Building .jar")
  (sh "jar" "cf" "resources/ruby-gems.jar" "-C" gems-dir "."))

(fs/delete-dir gems-dir)
(gem-install "sass" "-v" "3.4.25")
(gem-install "autoprefixer-rails")
(build-jar)
