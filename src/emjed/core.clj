(ns emjed.core
  (:gen-class)
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
;; handling

(defmacro jsonize [body]
 `(.replace
    (json/generate-string ~body {:pretty true})
    "\n" "\r\n"))

(defn- proc [cmd-and-args]
  (str
    (let [splits (re-seq #"[^ \t\r\n]+" cmd-and-args)
          cmd  (first splits)
          args (rest splits)]
      (try
        (cond
          ; general
          (= cmd "version") (str "emjed-" (get-version))
          (= cmd "pwd")     (ldb/pwd)
          (= cmd "cd")      (do (ldb/cd (first args)) "OK")
          (= cmd "load")    (do (ldb/load) "OK")
          (= cmd "save")    (do (ldb/save) "OK")
         ;(= cmd "export")
         ;(= cmd "import")
          ; libraries and programs
         ;(= cmd "register")
          (= cmd "pload")      (do (ldb/pload (keyword (first args)))
                                   "OK")
          (= cmd "registered") (jsonize (ldb/registered))
          (= cmd "unregister") (do (ldb/unregister (keyword (first args)))
                                   "OK")
          (= cmd "build")      (do (ldb/build (keyword (first args)))
                                   "OK")
          (= cmd "exec-fn")    (ldb/exec-fn (first args) (rest args))
          (= cmd "exec")       (ldb/exec (keyword (first args)))
          (= cmd "kill")       (do (ldb/kill
                                     (Integer/parseInt (first args)))
                                   "OK")
          (= cmd "ps")         (jsonize (ldb/ps))
          ; ldb conf
          (= cmd "get")     (jsonize (ldb/get (ldb/qk2kv (first args))))
          (= cmd "getrec")  (jsonize (ldb/getrec (ldb/qk2kv (first args))))
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
                          (catch Exception e (.toString e))) "\r\n"))
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
