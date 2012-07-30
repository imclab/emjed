(defproject emjed "0.1"
  :description "A program which manages programs for embedded computers"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/tools.logging "0.2.3"]
                 [server-socket "1.0.0"]
                 [org.clojars.tavisrudd/redis-clojure "1.3.1"]
                 [clj-json "0.5.1"]]
  :aot [clj-json.core
        redis.channel
        redis.connection
        redis.connection-pool
        redis.connection-timeout
        redis.core
        redis.defcommand
        redis.pipeline
        redis.protocol
        redis.vars
        server.socket
        clojure.tools.logging
        clojure.tools.logging.impl
        emjed.core]
  :resources-path "resources"
  :main emjed.core
)
