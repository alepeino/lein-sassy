(ns leiningen.lein-sassy.file-utils-spec
  (:require
    [clojure.java.io :as io]
    [clojure.test :refer [deftest testing is]]
    [leiningen.lein-sassy.file-utils :refer :all]))

(deftest sass-file?-test
  (testing "positive cases"
    (is (sass-file? (io/file "style.scss")))
    (is (sass-file? (io/file "style.sass"))))
  (testing "negative cases"
    (is (not (sass-file? (io/file "style.css"))))
    (is (not (sass-file? (io/file "foo.txt"))))
    (is (not (sass-file? (io/file ".foobar"))))))

(deftest sass-partial?-test
  (testing "positive cases"
    (is (sass-partial? (io/file "_include.scss")))
    (is (sass-partial? (io/file "includes/_include.sass"))))
  (testing "negative cases"
    (is (not (sass-partial? (io/file "include.sass"))))))

(deftest compilable-sass-file?-test
  (testing "positive cases"
    (is (compilable-sass-file? (io/file "test/files-in/sass/basic.sass")))
    (is (compilable-sass-file? (io/file "test/files-in/scss/basic.scss"))))
  (testing "negative cases"
    (is (not (compilable-sass-file? (io/file "test/files-compiled/basic_sass.css"))))
    (is (not (compilable-sass-file? (io/file "test/files-in/sass/_imported.sass"))))
    (is (not (compilable-sass-file? (io/file "test/files-in/scss"))))))

(deftest get-file-syntax-test
  (testing "return :syntax option if available"
    (is (= :sass (get-file-syntax (io/file "file.sass") {:syntax :sass})))
    (is (= :sass (get-file-syntax (io/file "file.scss") {:syntax :sass})))
    (is (= :scss (get-file-syntax (io/file "file.sass") {:syntax :scss}))))
  (testing "return keywordized file extension if no :syntax option"
    (is (= :sass (get-file-syntax (io/file "file.sass") {})))
    (is (= :scss (get-file-syntax (io/file "file.scss") {})))
    (is (= :sass (get-file-syntax (io/file "file.sass") {})))))

(deftest filename-to-css-test
  (testing "changes file extension to 'css'"
    (is (= "file.css" (filename-to-css (io/file "file.sass")))))
  (testing "preserves path"
    (is (= "dir/file.css" (filename-to-css (io/file "dir/file.sass"))))))

(deftest relative-path-test
  (testing "returns path relative to given root"
    (is (= "file.sass" (relative-path "dir/file.sass" "dir")))
    (is (= "subdir/file.sass" (relative-path "dir/subdir/file.sass" "dir"))))
  (testing "returns nil if root is not parent"
    (is (nil? (relative-path "dir/file.sass" "foo")))))

(deftest dest-path-test
  (testing "gets canonical path for destination file"
    (is (= "/public/css/file.css"
           (dest-path "/dir/sass" "/dir/sass/file.sass" "/public/css"))))
  (testing "trims path end slashes"
    (is (= "/public/css/file.css"
           (dest-path "/dir/sass/" "/dir/sass/file.sass" "/public/css/"))))
  (testing "preserves relative directory structure"
    (is (= "/public/css/subdir/sub.css"
           (dest-path "/dir/sass" "/dir/sass/subdir/sub.sass" "/public/css")))))
