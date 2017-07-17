(ns leiningen.integration-spec
  (:require
    [clojure.java.io :as io]
    [clojure.java.shell :refer [sh]]
    [clojure.test :refer :all]
    [me.raynes.fs :as fs]))

(defn sass [arg] (sh "lein" "with-profile" "example" "sass" arg))

(deftest integration
  (testing "'once' command renders correctly"
    (is (zero? (:exit (sass "once"))))
    (is (= (slurp "test/files-compiled/integration.css")
           (slurp "test/files-out/integration.css")))
    (is (= (slurp "test/files-compiled/integration.css.map")
           (slurp "test/files-out/integration.css.map"))))
  (testing "'clean' command deletes compiled files"
    (is (zero? (:exit (sass "clean"))))
    (is (zero? (count (fs/find-files "test/files-out" #".+\.css"))))
    (is (zero? (count (fs/find-files "test/files-out" #".+\.css\.map"))))))
