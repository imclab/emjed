(ns foo.core)

(defn -main [& args]
  (loop []
    (println "Foo: " (apply str args))
    (flush)
    (Thread/sleep 1000)
    (recur)))
