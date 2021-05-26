(defproject mult-mod "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [ubergraph "0.8.2"]
                 [juji/editscript "0.4.0"]
                 [loom-gorilla "0.1.0"]
                 [frankiesardo/linked "1.3.0"]
                 [robertluo/pull "0.2.10"]]
  :target-path "target/%s"
  :plugins [[org.clojars.benfb/lein-gorilla "0.6.0"]
            [cider/cider-nrepl "0.25.1"]]
  :profiles {:uberjar {:aot :all}})
