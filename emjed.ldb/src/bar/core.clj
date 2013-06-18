(ns bar.core)

(defn -main [& args]
  (println "Bar: " (apply str args)))

