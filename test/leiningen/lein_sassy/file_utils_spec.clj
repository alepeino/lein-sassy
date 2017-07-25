(ns leiningen.lein-sassy.file-utils-spec
  (:require
    [clojure.test :refer [deftest testing is]]
    [leiningen.lein-sassy.file-utils :refer :all]))

(deftest sass-file?-test
  (testing "positive cases"
    (is (sass-file? "style.scss"))
    (is (sass-file? "style.sass"))
    (is (sass-file? "subdir/style.sass")))
  (testing "negative cases"
    (is (not (sass-file? "style.css")))
    (is (not (sass-file? "foo.txt")))
    (is (not (sass-file? ".foobar")))))

(deftest sass-partial?-test
  (testing "positive cases"
    (is (sass-partial? "_include.scss"))
    (is (sass-partial? "includes/_include.sass")))
  (testing "negative cases"
    (is (not (sass-partial? "include.sass")))
    (is (not (sass-partial? "_includes/include.sass")))))

(deftest compilable-sass-file?-test
  (testing "positive cases"
    (is (compilable-sass-file? "test/files-in/basic.sass"))
    (is (compilable-sass-file? "test/files-in/subdir/in-subdir.sass"))
    (is (compilable-sass-file? "test/files-in/other-syntax.scss")))
  (testing "negative cases"
    (is (not (compilable-sass-file? "test/files-compiled/basic_sass.css")))
    (is (not (compilable-sass-file? "test/files-in/sass/_imported.sass")))
    (is (not (compilable-sass-file? "test/files-in/scss")))))

(deftest get-file-syntax-test
  (testing "returns :syntax option if available"
    (is (= :sass (get-file-syntax "file.sass" {:syntax :sass})))
    (is (= :sass (get-file-syntax "file.scss" {:syntax :sass})))
    (is (= :scss (get-file-syntax "file.sass" {:syntax :scss}))))
  (testing "defaults to keywordized file extension if no :syntax option"
    (is (= :sass (get-file-syntax "file.sass" {})))
    (is (= :scss (get-file-syntax "file.scss" {})))))

(deftest filename-to-css-test
  (testing "changes file extension to 'css'"
    (is (= "file.css" (filename-to-css "file.sass"))))
  (testing "preserves path"
    (is (= "dir/file.css" (filename-to-css "dir/file.sass")))))

(deftest relative-path-test
  (testing "returns path relative to given root"
    (is (= "file.sass" (relative-path "dir/file.sass" "dir")))
    (is (= "subdir/file.sass" (relative-path "dir/subdir/file.sass" "dir"))))
  (testing "returns the path unchanged if root is not parent"
    (is (= "dir/file.sass" (relative-path "dir/file.sass" "foo")))))

(deftest dest-path-test
  (testing "gets canonical path for destination file"
    (is (= "/public/css/file.css"
           (dest-path "/dir/sass/file.sass" "/dir/sass" "/public/css"))))
  (testing "trims path end slashes"
    (is (= "/public/css/file.css"
           (dest-path "/dir/sass/file.sass" "/dir/sass/" "/public/css/"))))
  (testing "preserves relative directory structure"
    (is (= "/public/css/subdir/sub.css"
           (dest-path "/dir/sass/subdir/sub.sass" "/dir/sass" "/public/css")))))
