(ns leiningen.lein-sassy.renderer-spec
  (:require
    [clojure.java.io :as io]
    [clojure.test :refer :all]
    [leiningen.lein-sassy.options :refer :all]
    [leiningen.lein-sassy.renderer :refer :all]))

(def project {:sass {:src "test/files-in/sass"
                     :dst "test/files-out/"
                     :syntax :sass
                     :style :expanded}})

(deftest render-file-test

  (testing "compiles basic .sass"
    (let [options (get-sass-options project)
          {:keys [container runtime]} (init-renderer options)]
      (is (= [(slurp "test/files-compiled/basic_sass.css") nil]
             (render-file container runtime options (io/file "test/files-in/sass/basic.sass") "")))))

  (testing "compiles compressed .sass"
    (let [options (get-sass-options (-> project (assoc-in [:sass :style] :compressed)))
          {:keys [container runtime]} (init-renderer options)]
      (is (= [(slurp "test/files-compiled/basic_sass.min.css") nil]
             (render-file container runtime options (io/file "test/files-in/sass/basic.sass") "")))))

  (testing "compiles basic .scss"
    (let [options (get-sass-options (-> project (assoc-in [:sass :syntax] :scss)
                                                (assoc-in [:sass :src] "test/files-in/scss")))
          {:keys [container runtime]} (init-renderer options)]
      (is (= [(slurp "test/files-compiled/basic_scss.css") nil]
             (render-file container runtime options (io/file "test/files-in/scss/basic.scss") "")))))

  (testing "compiles compressed .scss"
    (let [options (get-sass-options (-> project (assoc-in [:sass :syntax] :scss)
                                                (assoc-in [:sass :src] "test/files-in/scss")
                                                (assoc-in [:sass :style] :compressed)))
          {:keys [container runtime]} (init-renderer options)]
      (is (= [(slurp "test/files-compiled/basic_scss.min.css") nil]
             (render-file container runtime options (io/file "test/files-in/scss/basic.scss") "")))))

  (testing "sourcemap in file"
    (let [options (get-sass-options (-> project (assoc-in [:sass :sourcemap] :auto)))
          {:keys [container runtime]} (init-renderer options)]
      (is (= [(slurp "test/files-compiled/sourcemap-file.css")
              (slurp "test/files-compiled/sourcemap-file.css.map")]
             (render-file container runtime options
                          (io/file "test/files-in/sass/basic.sass")
                          "test/files-compiled/sourcemap-file.css")))))

  (testing "sourcemap inline"
    (let [options (get-sass-options (-> project (assoc-in [:sass :sourcemap] :inline)))
          {:keys [container runtime]} (init-renderer options)]
      (is (= [(slurp "test/files-compiled/sourcemap-inline.css")
              nil]
             (render-file container runtime options
                          (io/file "test/files-in/sass/basic.sass")
                          "test/files-compiled/sourcemap-inline.css")))))

  (testing "autoprefixer plugin, no sourcemap"
    (let [options (get-sass-options (-> project (assoc-in [:sass :plugins] [:autoprefixer])))
          {:keys [container runtime]} (init-renderer options)]
      (is (= [(slurp "test/files-compiled/autoprefixer-no-sourcemap.css")
              nil]
             (render-file container runtime options
                          (io/file "test/files-in/sass/autoprefixer.sass")
                          "test/files-compiled/autoprefixer-no-sourcemap.css")))))

  (testing "autoprefixer plugin, sourcemap in separate file"
    (let [options (get-sass-options (-> project (assoc-in [:sass :plugins] [:autoprefixer])
                                                (assoc-in [:sass :sourcemap] :auto)))
          {:keys [container runtime]} (init-renderer options)]
      (is (= [(slurp "test/files-compiled/autoprefixer-sourcemap-auto.css")
              (slurp "test/files-compiled/autoprefixer-sourcemap-auto.css.map")]
             (render-file container runtime options
                          (io/file "test/files-in/sass/autoprefixer.sass")
                          "test/files-compiled/autoprefixer-sourcemap-auto.css")))))

  (testing "autoprefixer plugin, sourcemap inline"
    (let [options (get-sass-options (-> project (assoc-in [:sass :plugins] [:autoprefixer])
                                                (assoc-in [:sass :sourcemap] :inline)))
          {:keys [container runtime]} (init-renderer options)]
      (is (= [(slurp "test/files-compiled/autoprefixer-sourcemap-inline.css")
              nil]
             (render-file container runtime options
                          (io/file "test/files-in/sass/autoprefixer.sass")
                          "test/files-compiled/autoprefixer-sourcemap-inline.css"))))))
