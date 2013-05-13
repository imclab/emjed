(defproject org.clojars.kohyama/emjed "1.0.1"
  :description "A program which manages programs for embedded computers"
  :url "https://github.com/kohyama/emjed"
  :dev-dependencies [[org.clojure/algo.monads "0.1.0"]]
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/core.incubator "0.1.2"]
                 [org.clojure/tools.logging "0.2.3"]
                 [server-socket "1.0.0"]
                 [overtone/at-at "1.0.0"]
                 [cheshire "5.0.1"]]
  :aot [clojure.core.incubator
        clojure.tools.logging
        clojure.tools.logging.impl
        server.socket
        overtone.at-at
        cheshire.core
        cheshire.custom
        cheshire.factory
        cheshire.generate
        cheshire.parse
        cheshire.monad]
  :omit-source true
  :main emjed.core
)
