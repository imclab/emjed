(ns emjed.utils)

;; Thanks Mr. athos0220
(defmacro eval-when-compile [& body]
  (binding [*compile-files* false]
    (eval `(do ~@body)))
  nil)
