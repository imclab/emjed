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
;             INTERVAL
;   interval    (integer [minute])
;   name-spaces (string)
;   main        (string)
;   args        (vector of string or number)
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

(def runnings (ref {}))
(def tp (at-at/mk-pool))

(defmacro register [p-name-kw attr]
 `(swap! prog assoc ~p-name-kw ~attr))

(defmacro registered []
 `@prog)

(defmacro unregister [p-name-kw]
 `(swap! prog dissoc ~p-name-kw))

(defmacro build [p-name-kw]
 `(let [f# (file (str @dir "/classes"))]
    (if (and (.exists f#) (.isFile f#))
        "A Plain File named \"classes\" already exists"
        (do
          (if (not (.exists f#)) (.mkdir f#))
          (binding [*compile-path* (.getCanonicalPath f#)]
            (let [prog-attr# (~p-name-kw @prog)
                  name-spaces# (:name-spaces prog-attr#)]
              (doseq [name-space# name-spaces#]
                (compile (symbol name-space#)))
              (if-let [main# (:main prog-attr#)]
                (if (not (some #(= % main#) name-spaces#))
                    (compile (symbol main#))))))))))

(defmacro pload [p-name-kw]
 `(let [{onss# :name-spaces mns# :main} (~p-name-kw @prog)
        nss# (if mns# (cons mns# onss#) onss#)]
    (doseq [name-space# nss#]
      (if name-space#
        (require (symbol name-space#) :reload)))))

; for test
(defmacro exec-fn [fqf args] ; note args is a list
 `(apply (resolve (symbol ~fqf)) ~args))

; internal interface
(defmacro add-running [p-name-kw fqf p-sj args]
 `(dosync
    (let [pid# (first (drop-while
                        (fn [c#] (some #(= c# %) (keys @runnings)))
                        (range)))]
      (alter runnings assoc pid# {:name (name ~p-name-kw)
                                  :start-at (Date.)
                                  :function ~fqf
                                  :args ~args
                                  :sj ~p-sj}) ; scheduled job
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
 `(let [{t# :timing i# :interval
         main-ns# :main args# :args :as attr#}
          (~p-name-kw @prog)]
    (cond
      (nil? attr#) (str "Can't find program named: " (name ~p-name-kw))
      (= t# "ONCE")
        (let [fqf# (str main-ns# "/-main")
              f# (resolve (symbol fqf#))]
          (add-running ~p-name-kw fqf#
            (future (apply f# args#))
            args#)) ; returns pid
      (= t# "LOOP")
        (let [fqf# (str main-ns# "/-main")
              f# (create-loop fqf#)]
          (add-running ~p-name-kw fqf#
            (future (apply f# args#))
            args#)) ; returns pid
      (= t# "INTERVAL")
        (let [fqf# (str main-ns# "/-main")
              f# (resolve (symbol fqf#))]
          (add-running ~p-name-kw fqf#
            (at-at/every (* i# 60000) #(apply f# args#) tp
                         :initial-delay (mod (- (at-at/now)) 60000))
            args#)))))

(defmacro kill [pid]
 `(dosync
    (if-let [p# (@runnings ~pid)]
      (if-let [sj# (p# :sj)]
        (cond
          (future? sj#)
            (do
              (alter runnings dissoc ~pid)
              (if-not (future-cancel sj#)
                (throw (Exception. (str "Can't Stop Process: " ~pid)))))
          (= (class sj#) overtone.at_at.RecurringJob)
            (do
              (alter runnings dissoc ~pid)
              (if-not (at-at/stop sj#)
                (throw (Exception. (str "Can't Stop Process: " ~pid)))))))
      (throw (Exception. (str "No Such Process: " ~pid))))))

(defmacro ps []
 `(let [stat# #(if-let [sj# (:sj %)]
                 (cond
                   (future? sj#)
                     (cond
                       (not (future? %)) "Unknown status"
                       (future-cancelled? %) "Cancelled"
                       (future-done? %) "Done"
                       :else "Running")
                   (= (class sj#) overtone.at_at.RecurringJob)
                     (if @(:scheduled? sj#)
                         "Running"
                         "Suspended")
                   :else "Unknown type of process"))]
    (map (fn [[pid# p#]]
           (dissoc (assoc p# :pid pid# :state (stat# p#)) :sj))
         @runnings)))

