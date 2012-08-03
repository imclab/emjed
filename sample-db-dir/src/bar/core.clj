(ns bar.core)

(defn -main [& args]
  (loop []
    (println "Bar: " (apply str args))
    (flush)
    (Thread/sleep 1000)
    (recur)))
