(in-ns 'emjed.ldb)

;; ----------------------------------------------------------------
;; prog
;; libraries and executables.
;; An executable has an entry point function.
;; A name of entry point function should be -main

; register
;   name
;   execution AUTO | MANUAL
;   timing    LOOP | ONCE | ["****-**-** *** **:**:**", ]
;   name-spaces (string)
;   main        (string)
;   args        (string vector)
;
; unregister
; build
;
; load
; reload
; unload?
;
; exec-fn arbitrary function
; exec program

(def ^:dynamic *runnings* (ref {}))

(defmacro on-start []
  ; *prog* にロードされた後
  ; 自動実行するプログラムを実行する.
)

(defmacro register [p-name-kw attr]
 `(swap! *prog* assoc ~p-name-kw ~attr))

(defmacro registered []
 `@*prog*)

(defmacro unregister [p-name-kw]
 `(swap! *prog* dissoc ~p-name-kw))

(defmacro build [p-name-kw]
 `(let [f# (file (str @*dir* "/classes"))]
    (if (and (.exists f#) (.isFile f#))
        "A Plain File named \"classes\" already exists"
        (do
          (if (not (.exists f#)) (.mkdir f#))
          (binding [*compile-path* (.getCanonicalPath f#)]
            (let [prog-attr# (~p-name-kw @*prog*)
                  name-spaces# (:name-spaces prog-attr#)]
              (doseq [name-space# name-spaces#]
                (compile (symbol name-space#)))
              (if-let [main# (:main prog-attr#)]
                (if (not (some #(= % main#) name-spaces#))
                    (compile (symbol main#))))))))))

; for test
; should be included some function?
(defmacro pload [p-name-kw]
 `(doseq [name-space# (:name-spaces (~p-name-kw @*prog*))]
    (require (symbol name-space#) :reload)))

; for test
(defmacro exec-fn [fqf args] ; note args is a list
 `(apply (resolve (symbol ~fqf)) ~args))

; internal interface
(defmacro add-running [p-name-kw fqf p-future args]
 `(dosync
    (let [pid# (first (drop-while
                        (fn [c#] (some #(= c# %) (keys @*runnings*)))
                        (range)))]
      (alter *runnings* assoc pid# {:name (name ~p-name-kw)
                                    :start-at (Date.)
                                    :function ~fqf
                                    :args ~args
                                    :future ~p-future})
      pid#)))

(defmacro create-loop [fqf]
 `(fn [& args#]
    (loop []
      (when
        (not= :ie
          (try
            (apply (resolve (symbol ~fqf)) args#)
            (Thread/sleep 1000)
            (catch InterruptedException ie# :ie)))
        (recur)))))

(defmacro exec [p-name-kw]
 `(let [{t# :timing main-ns# :main args-str# :args :as attr#}
          (~p-name-kw @*prog*)
        args# (if (nil? args-str#) nil (re-seq #"[^ \t]+" args-str#)) ]
    (cond
      (nil? attr#) (str "Can't find program named: " (name ~p-name-kw))
      (= t# "ONCE") (let [fqf# (str main-ns# "/-main")
                         f# (resolve (symbol fqf#))]
                      (add-running ~p-name-kw fqf#
                        (future (apply f# args#))
                        args#)) ; returns pid
      (= t# "LOOP") (let [fqf# (str main-ns# "/-main")
                          f# (create-loop fqf#)]
                      (add-running ~p-name-kw fqf#
                        (future (apply f# args#))
                        args#)) ; returns pid
    )
  )
)

(defmacro kill [pid]
 `(dosync
      (if-let [f# (get-in @*runnings* [~pid :future])]
        (if-not (future-cancel f#)
                (throw (Exception. (str "Can't Stop Process: " ~pid))))
        (throw (Exception. (str "No Such Process: " ~pid))))
      (alter *runnings* dissoc ~pid)))

(defmacro ps []
 `(let [stat# #(cond
                (not (future? %)) "?"
                (future-cancelled? %) "Cancelled"
                (future-done? %) "Done"
                :else "Running")]
    (map (fn [[pid# p#]]
           (dissoc (assoc p# :pid pid# :state (stat# (:future p#)))
             :future))
         @*runnings*)))

