(in-ns emjed.core)

(require '[redis.core :as redis])

(defmacro redis-do [& body]
  `(redis/with-server {:host "localhost" :port 6379 :db 0} ~@body))

(defn ldb-get [k]
  (let [tik (str k "^type")]
    (redis-do
      (cond
        (not (redis/exists k))          :no-existent-key
        (not (redis/exists tik))        :incorrect-key
        (not= (redis/type tik) :string) :incorrect-key
        (= (redis/type k) :string)
          (let [ti (redis/get tik)]
            (if (or (= ti "number") (= ti "string"))
                (redis/get k)
                :incorrect-key))
        (= (redis/type k) :list)
          (let [ti (redis/get tik)]
            (if (= ti "hash-map")
                (redis/lrange k 0 -1)
                :incorrect-key))
        :else :incorrect-key))))
