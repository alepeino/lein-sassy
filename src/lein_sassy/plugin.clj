(ns lein-sassy.plugin
  (:require
    [leiningen.clean]
    [leiningen.compile]
    [leiningen.sass :as sass]
    [leiningen.lein-sassy.options :refer [get-sass-options]]
    [robert.hooke :as hooke]))

(defn- clean-hook [task & args]
  (apply task args)
  (when-let [options (sass/get-sass-options (first args))]
    (sass/clean options)))

(defn- compile-hook [task & args]
  (apply task args)
  (when-let [options (sass/get-sass-options (first args))]
    (sass/once options)))

(defn hooks []
  (hooke/add-hook #'leiningen.clean/clean #'clean-hook)
  (hooke/add-hook #'leiningen.compile/compile #'compile-hook))

(defn activate
  "Plugin hooks, Leiningen 1.x compatibility"
  []
  (hooks))
