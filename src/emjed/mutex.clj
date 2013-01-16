(ns emjed.mutex
  (:refer-clojure :exclude (get)))

(def mutexes (ref {}))
(defn get [i]
  (dosync
    (if (nil? (@mutexes i))
        (alter mutexes assoc i (atom 0)))
    (@mutexes i)))

(defn lock [m] (compare-and-set! m 0 1))

(defn unlock [m] (compare-and-set! m 1 0))

(defn wait-and-lock [m]
  (loop []
    (if (compare-and-set! m 0 1)
        nil
        (do (Thread/sleep 500) (recur)))))
