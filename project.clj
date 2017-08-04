(defproject lein-sassy "1.0.8"

  :description "Use Sass with Clojure."

  :url "https://github.com/vladh/lein-sassy"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :scm {:name "git"
        :url "https://github.com/vladh/lein-sassy"}

  :repositories [["torquebox" "http://rubygems-proxy.torquebox.org/releases"]]

  :dependencies [[hawk "0.2.11"]
                 [me.raynes/fs "1.4.6"]
                 [zweikopf "1.0.2"]]

  :profiles {:provided {:dependencies [[rubygems/sass "3.4.25" :extension "gem"]]
                        :filespecs [{:type :path :path "target/rubygems-provided"}]}

             :dev {:plugins [[big-solutions/lein-mvn "0.1.0"]]
                   :clean-targets ["target/classes" "target/stale"]
                   :clean-non-project-classes false
                   :aliases {"gems" ["do" "pom" ["mvn" "clean"] ["mvn" "initialize"]]}}

             :example {:sass {:src "test/files-in"
                              :dst "test/files-out"
                              :sourcemap :auto
                              :style :expanded
                              :delete-output-dir false
                              :cache false}}}

  :pom-plugins [[de.saumya.mojo/gem-maven-plugin "1.0.0"
                 {:configuration
                    [:includeRubygemsInResources "true"]
                  :executions
                    ([:execution
                      [:goals
                       ([:goal "initialize"])]])}]]

  :eval-in-leiningen true)
