(ns leiningen.integration-spec
  (:require
    [clojure.java.shell :refer [sh]]
    [clojure.test :refer :all]
    [me.raynes.fs :as fs]))

(defn sass [arg] (sh "lein" "with-profile" "example" "sass" arg))

(deftest integration

  (testing "'once' command renders correctly"
    (is (zero? (:exit (sass "once"))))

    (testing "integration file with sourcemap"
      (is (= (slurp "test/files-compiled/integration.css")
             (slurp "test/files-out/integration.css")))
      (is (= (slurp "test/files-compiled/integration.css.map")
             (slurp "test/files-out/integration.css.map"))))

    (testing "file in subdir with same relative path"
      (is (= (slurp "test/files-compiled/subdir/in-subdir.css")
             (slurp "test/files-out/subdir/in-subdir.css")))
      (is (= (slurp "test/files-compiled/subdir/in-subdir.css.map")
             (slurp "test/files-out/subdir/in-subdir.css.map")))))

  (testing "'clean' command deletes compiled files"
    (is (zero? (:exit (sass "clean"))))
    (is (zero? (count (fs/find-files* "test/files-out" (comp #{".css" ".map"} fs/extension)))))))
