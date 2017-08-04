(ns leiningen.lein-sassy.renderer-spec
  (:require
    [clojure.test :refer :all]
    [leiningen.lein-sassy.renderer :refer :all]
    [leiningen.sass :refer [get-sass-options init-ruby-context]]))

(def options
  (get-sass-options {:sass {:src "test/files-in"
                            :dst "test/files-out"}}))

(use-fixtures :each
              (fn [t]
                (init-ruby-context options)
                (t)))

(deftest render-file-test

  (testing "compiles basic .sass"
    (is (= [(slurp "test/files-compiled/basic.css") nil]
           (render-file "test/files-in/basic.sass" options))))

  (testing "compiles file with imports"
    (is (= [(slurp "test/files-compiled/imports.css") nil]
           (render-file "test/files-in/imports.sass" options))))

  (testing "compiles .scss"
    (is (= [(slurp "test/files-compiled/other-syntax.css") nil]
           (render-file "test/files-in/other-syntax.scss" options))))

  (testing "compressed style"
    (is (= [(slurp "test/files-compiled/basic.min.css") nil]
           (render-file "test/files-in/basic.sass" (assoc options :style :compressed)))))

  (testing "sourcemap :auto"
    (is (= [(slurp "test/files-compiled/sourcemap-auto.css")
            (slurp "test/files-compiled/sourcemap-auto.css.map")]
           (render-file "test/files-in/sourcemap-auto.sass" (assoc options :sourcemap :auto)))))

  (testing "sourcemap :inline"
    (is (= [(slurp "test/files-compiled/sourcemap-inline.css") nil]
           (render-file "test/files-in/sourcemap-inline.sass" (assoc options :sourcemap :inline))))))
