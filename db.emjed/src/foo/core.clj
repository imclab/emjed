(ns foo.core)

(defn -main [& args]
  (println "Foo: " (apply str args))
  (flush))
