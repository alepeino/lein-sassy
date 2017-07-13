(ns leiningen.lein-sassy.options-spec
  (:require [clojure.test :refer :all]
            [leiningen.lein-sassy.options :refer :all]
            [leiningen.lein-sassy.renderer :refer :all]))

(def default-options {:src "resources/public/stylesheets"
                      :dst "resources/app/stylesheets"
                      :delete-output-dir true
                      :style :nested})

(def project {:sass {:src "test/files-in/sass"
                     :dst "test/files-out/"
                     :syntax :sass
                     :style :expanded}})

(deftest get-sass-options-test

  (testing "merges project with defaults, adding default load paths"
    (is (= (merge
             default-options
             {:load_paths [(get-in project [:sass :src])]}
             (:sass project))
           (get-sass-options project))))

  (testing "throws warning when no sass entry in project"
    (binding [*err* (java.io.StringWriter.)]
      (get-sass-options {:foo "bar"})
      (is (= "No sass entry found in project definition.\n" (str *err*))))))
