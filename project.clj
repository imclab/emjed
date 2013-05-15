(defproject org.clojars.kohyama/emjed "1.0.1"
  :description "A program which manages programs for embedded computers"
  :url "https://github.com/kohyama/emjed"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/core.incubator "0.1.2"]
                 [org.clojure/tools.logging "0.2.3"]
                 [server-socket "1.0.0"]
                 [overtone/at-at "1.0.0"]
                 [org.clojure/data.json "0.2.2"]]
  :aot :all
  :omit-source true
  :main emjed.core)
