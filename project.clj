(defproject lein-sassy "1.0.8"
  :description "Use Sass with Clojure."
  :url "https://github.com/vladh/lein-sassy"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :scm {:name "git"
        :url "https://github.com/vladh/lein-sassy"}

  :dependencies [[com.cemerick/pomegranate "0.3.1"]
                 [hawk "0.2.11"]
                 [me.raynes/fs "1.4.6"]
                 [org.jruby/jruby-complete "9.1.12.0"]]

  :profiles {:dev {:plugins [[lein-exec "0.3.6"]]}
             :example {:sass {:src "test/files-in/integration"
                              :dst "test/files-out"
                              :sourcemap :auto
                              :delete-output-dir false}}}

  :eval-in-leiningen true

  :aliases {"gem-install" ["exec" "-p" "src/leiningen/lein_sassy/gem_install.clj"]})
