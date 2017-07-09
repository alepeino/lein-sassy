(ns leiningen.lein-sassy.renderer-spec
  (:require [clojure.test :refer :all]
            [leiningen.lein-sassy.options :refer :all]
            [leiningen.lein-sassy.renderer :refer :all]))

(def default-options {:src "resources/public/stylesheets"
                      :dst "resources/app/stylesheets"
                      :gem-name "sass"
                      :gem-version "3.2.14"
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

(deftest render-file-test

  (testing "compiles basic .sass"
    (let [options (get-sass-options project)
          {:keys [container runtime]} (init-renderer options)]
      (is (= (slurp "test/files-compiled/basic_sass.css")
             (render-file container runtime options (str (:src options) "/basic.sass"))))))

  (testing "compiles compressed .sass"
    (let [options (get-sass-options (-> project (assoc-in [:sass :style] :compressed)))
          {:keys [container runtime]} (init-renderer options)]
      (is (= (slurp "test/files-compiled/basic_sass.min.css")
             (render-file container runtime options (str (:src options) "/basic.sass"))))))

  (testing "compiles basic .scss"
    (let [options (get-sass-options (-> project (assoc-in [:sass :syntax] :scss)
                                                (assoc-in [:sass :src] "test/files-in/scss")))
          {:keys [container runtime]} (init-renderer options)]
      (is (= (slurp "test/files-compiled/basic_scss.css")
             (render-file container runtime options (str (:src options) "/basic.scss"))))))

  (testing "compiles compressed .scss"
    (let [options (get-sass-options (-> project (assoc-in [:sass :syntax] :scss)
                                                (assoc-in [:sass :src] "test/files-in/scss")
                                                (assoc-in [:sass :style] :compressed)))
          {:keys [container runtime]} (init-renderer options)]
      (is (= (slurp "test/files-compiled/basic_scss.min.css")
             (render-file container runtime options (str (:src options) "/basic.scss")))))))
