(defproject org.clojars.jj/archive "1.1.3-SNAPSHOT"
  :description "Leiningen plugin to effortlessly create archives of your project files. Supports tgz and zip formats"
  :url "https://github.com/ruroru/lein-archive"
  :license {:name "EPL-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}

  :dependencies [[org.clojure/clojure "1.12.3"]
                 [org.apache.commons/commons-compress "1.28.0"]]

  :deploy-repositories [["clojars" {:url      "https://repo.clojars.org"
                                    :username :env/clojars_user
                                    :password :env/clojars_pass}]]

  :profiles {:test {:global-vars {*warn-on-reflection* true}}}

  :plugins [[org.clojars.jj/bump "1.0.4"]
            [org.clojars.jj/strict-check "1.1.0"]
            [org.clojars.jj/bump-md "1.1.0"]]

  :repl-options {:init-ns archive.core})
