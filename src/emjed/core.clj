(ns emjed.core
  (:gen-class)
  (:import  [java.util Date])
  (:use     [clojure.java.io]
            [server.socket])
  (:require [emjed.ldb :as ldb]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]))

;; ----------------------------------------------------------------
;; general
;;
(defmacro get-version []
  (System/getProperty "emjed.version"))

;; ----------------------------------------------------------------
;; programs
;;
(def ^:dynamic *runnings* (atom {}))

(defn- p-require [p-main]
  (require (symbol p-main))
  "OK")

(defn- p-compile [p-main & p-other-namespaces]
  ; TODO if p-classes doesn't exist, create it
  (binding [*compile-path*
              (.getCanonicalPath (file (str (ldb/pwd) "/classes")))]
    (doall (map #(compile (symbol %)) p-other-namespaces))
    (compile (symbol p-main))
  )
  "OK")

(defn- exec [p-fn & other-args]
  (if-let [f (resolve (symbol p-fn))]
    (do
      (swap! *runnings*
        (fn [runnings]
          (let [pid (first
                      (drop-while
                        (fn [c] (some #(= c %) (keys runnings)))
                        (range)))]
            (assoc runnings pid {
              :start-at (Date.)
              :function p-fn
              :args other-args
              :future (future (apply f other-args))}))))
      "OK")
      (str "Can't resolve function: " p-fn)))

(defn- kill [str-pid]
  (let [pid (Integer/parseInt str-pid)]
    (swap! *runnings*
      (fn [runnings]
        (if-let [f (get-in runnings [pid :future])]
          (if-not (future-cancel f)
                  (throw (Exception. (str "Can't Stop Process: " pid))))
          (throw (Exception. (str "No Such Process: " pid))))
        (dissoc runnings pid)))
    "OK"))

(defn- ps []
  (let [stat #(cond
                (not (future? %)) "?"
                (future-cancelled? %) "Cancelled"
                (future-done? %) "Done"
                :else "Running")]
    (->> @*runnings*
      (map
        (fn [[pid p]]
          (str "  {\"pid\":      " pid ",\r\n"
               "   \"start-at\": \"" (:start-at p) "\",\r\n"
               "   \"status\":   \"" (stat (:future p)) "\",\r\n"
               "   \"cmd\":      \""
                 (:name (:program p))
                 (apply str (mapcat #(list " " %) (:args p)))
               "\"}")))
      (interpose ",\r\n")
      (apply str)
      (#(str "{\r\n" % "\r\n}")))))


;; ----------------------------------------------------------------
;; handling

(defn- proc [cmd-and-args]
  (str
    (let [splits (re-seq #"[^ \t\r\n]+" cmd-and-args)
          cmd  (first splits)
          args (rest splits)]
      (try
        (cond
          ; general
          (= cmd "version") (str "emjed-" (get-version))
         ;(= cmd "export")  (apply c-export args)
         ;(= cmd "import")  (apply c-import args)
          ; libraries and programs
          (= cmd "compile") (apply p-compile args)
          (= cmd "require") (apply p-require args)
          (= cmd "exec")    (apply exec args)
          (= cmd "kill")    (apply kill args)
          (= cmd "ps")      (ps)
          ; ldb general
          (= cmd "pwd")     (ldb/pwd)
          (= cmd "cd")      (do (ldb/cd (first args)) "OK")
          (= cmd "load")    (do (ldb/load) "OK")
          (= cmd "save")    (do (ldb/save) "OK")
          ; ldb conf
          (= cmd "get")     (json/generate-string
                              (ldb/get (ldb/qk2kv (first args))))
          (= cmd "getrec")  (json/generate-string
                              (ldb/getrec (ldb/qk2kv (first args)))
                              {:pretty true})
          (= cmd "set")     (do (ldb/set
                                  (ldb/qk2kv (first args))
                                    (json/parse-string (second args) true))
                                "OK")
          (= cmd "del")     (do (ldb/del (ldb/qk2kv (first args))) "OK")
          (= cmd "rename")  (do (ldb/rename (ldb/qk2kv (first args))
                                            (ldb/qk2kv (second args)))
                                "OK")
          :else (str cmd ": command not found."))
        (catch Exception e (.toString e))))
    "\r\n"))

(defn- fproc [cmd-and-args in out rdr wtr]
  (str
    (let [splits (re-seq #"[^ \t\r\n]+" cmd-and-args)
          cmd  (first splits)
          args (rest splits)]
      (try
        (cond
          (= cmd "fget") (let [ba (ldb/fget (first args))
                               len (count ba)]
                           (.write wtr (str len "\r\n"))
                           (.flush wtr)
                           (.write out ba 0 (count ba))
                           "")
          (= cmd "fput") (let [len (Integer/parseInt (second args))
                               ba (byte-array len)]
                           ; TODO loop with a timeout
                           (.read in ba 0 len)
                           (.readLine rdr)
                           (ldb/fput (first args) ba)
                           "OK")
          (= cmd "fdel") (do (ldb/fdel (first args)) "OK")
          :else (str cmd ": command not found."))
        (catch Exception e (.toString e))))
  "\r\n"))

(defn- handler [in out]
  (with-open [rdr (reader in) wtr (writer out)]
    (ldb/cd)
    (loop [lines (.readLine rdr)]
      (cond
        (= (apply str (take 5 lines)) "close") nil
        (= (last lines) \\) (recur (str lines "\r\n" (.readLine rdr)))
        (= (first lines) \`)
          (do (.write wtr
                (str (try (->> (rest lines) (apply str) (load-string) (str))
                          (catch Exception e (.toString e)))))
              (.flush wtr)
              (recur (.readLine rdr)))
        (= (first lines) \f)
          (do (.write wtr (fproc lines in out rdr wtr))
              (.flush wtr)
              (recur (.readLine rdr)))
        :else
          (do (.write wtr (proc lines))
              (.flush wtr)
              (recur (.readLine rdr)))))))

(def ^:dynamic *server* (atom nil))

(defn- start []
  (reset! *server* (create-server 3000 handler)))

(defn- stop []
  (when-not (nil? @*server*)
    (close-server @*server*)
    (reset! *server* nil)))

(defn -main [& args]
  (if-let [path (first args)]
    (ldb/cd path)
    (ldb/cd "."))
  (start))
