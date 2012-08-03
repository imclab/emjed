(defproject emjed "0.1"
  :description "A program which manages programs for embedded computers"
  :dev-dependencies [[org.clojure/algo.monads "0.1.0"]]
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/core.incubator "0.1.0"]
                 [org.clojure/tools.logging "0.2.3"]
                 [server-socket "1.0.0"]
                 [cheshire "4.0.1"]]
  :aot [clojure.core.incubator
        clojure.tools.logging
        clojure.tools.logging.impl
        server.socket
        cheshire.core
        cheshire.custom
        cheshire.factory
        cheshire.generate
        cheshire.parse
        cheshire.monad]
  :omit-source true
  :resources-path "resources"
  :main emjed.core
)
