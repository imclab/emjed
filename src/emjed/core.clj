(ns emjed.core
  (:gen-class)
  (:use     [clojure.java.io]
            [clojure.string :only [join split]]
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

(defmacro bs-apply [s]
 `(loop [c# ~s]
    (let [r# (join (split c# #"[^\u0008]\u0008"))]
      (if (= r# c#) r# (recur r#)))))

(defn- proc [line in out rdr wtr]
  (str
    (let [splits (re-seq #"[^ \t\r\n]+" line)
          cmd  (first splits)
          args (rest splits)]
      (try
        (cond
          ; escape
          (= (first line) \`)
            (try (->> (rest line) (apply str) (load-string) (str))
                 (catch Exception e (.toString e)))
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
          ; ldb file
          (= cmd "flist")   (jsonize (ldb/flist))
          (= cmd "fget")    (let [ba (ldb/fget (first args))
                                  len (count ba)]
                              (.write wtr (str len "\r\n"))
                              (.flush wtr)
                              (.write out ba 0 len)
                              "")
          (= cmd "ftget")   (apply str
                              (interleave
                                (re-seq #"[^\r\n]+"
                                  (slurp (str (ldb/pwd) "/files/"
                                    (first args))))
                                (repeat "\r\n")))
          (= cmd "fput")    (let [len (Integer/parseInt (second args))
                                  ba (byte-array len)]
                              ; TODO loop with a timeout
                              (.read in ba 0 len)
                              (.readLine rdr)
                              (ldb/fput (first args) ba)
                              "OK")
          (= cmd "ftput")   (let [fname (str (ldb/pwd) "/files/" (first args))
                                  emark (second args)]
                              (loop [line (.readLine rdr) lines ""]
                                (if (or (= line emark)
                                        (= (int (first line)) 0x04))
                                    (spit fname lines)
                                    (recur (.readLine rdr)
                                           (str lines "\r\n" line))))
                              "OK")
          (= cmd "fdel")    (do (ldb/fdel (first args)) "OK")
          (= cmd "frename") (do (ldb/frename (first args) (second args)) "OK")
          :else (str cmd ": command not found."))
        (catch Exception e (.toString e))))
    "\r\n"))

; ----------------------------------------------------------------
;  fput と fget だけ特殊扱いすればいい
(defn- telnet-handler [in out]
  (with-open [rdr (reader in) wtr (writer out)]
    (loop [line (bs-apply (.readLine rdr))]
      (if (= (apply str (take 5 line)) "close")
          nil
          (do (.write wtr (proc line in out rdr wtr))
              (.flush wtr)
              (recur (bs-apply (.readLine rdr))))))))

; ----------------------------------------------------------------
(defn- decode-url-encoded [s]
  (apply str
    (loop [a [] [c & r] s]
      (cond
        (nil? c) a
        (= c \%)
          (let [[t o & u] r]
            (recur (conj a (char (Integer/parseInt (str t o) 16))) u))
        :else (recur (conj a c) r)))))

(defn- http-header-parse [lines]
  (try
    (let [[firstline & headers]    (split lines #"\r\n")
          [[_ method _ proto host _ port path _ params httpv]]
            (re-seq
  #"(\S+)\s+(([^:\s]+)://)?([^/:\s]*)(:([^/\s]*))?([^\?\s]+)(\?(\S+))?\s+(\S+)"
  ; method  .proto         host      . port       path      .  params    ver
              firstline)]
      {:method method :proto proto :host host :port port :path path
       :httpv httpv
       :headers (if (nil? headers) nil
                    (into {} (for [p headers
                                   :let [[s v] (split p #": *")]]
                               [(keyword s) v])))
      ;:params (if (nil? params) nil
      ;            (into {}
      ;              (for [p (split params #"&")
      ;                   :let [[s v] (split p #"=")]]
      ;                [(keyword s) v])))
       :params (decode-url-encoded params)
       })
    (catch Exception e (.printStackTrace e) nil)))

(defn- http-method-not-allowed [wtr]
  (let [mes "<html><body><h1>405 Method Not Allowed</h1></body></html>"]
    (.write wtr
      (str "HTTP/1.1 405 Method Not Allowed\r\n"
           "Content-Length: " (+ 2 (count mes)) "\r\n\r\n"
           mes "\r\n"))
    (.flush wtr)))

(defn- http-bad-request [wtr]
  (let [mes "<html><body><h1>400 Bad Request</h1></body></html>"]
    (.write wtr
      (str "HTTP/1.1 400 Bad Request\r\n"
           "Content-Length: " (+ 2 (count mes)) "\r\n\r\n"
           mes "\r\n"))
    (.flush wtr)))

(defn- http-not-found [wtr]
  (let [mes "<html><body><h1>404 Not Found</h1></body></html>"]
    (.write wtr
      (str "HTTP/1.1 404 Not Found\r\n"
           "Content-Length: " (+ 2 (count mes)) "\r\n\r\n"
           mes "\r\n"))
    (.flush wtr)))

(defn- http-proc-get [{path :path params :params} in out rdr wtr]
  (if (not= path "/")
      (let [ba (try (ldb/fget (apply str (rest path)))
                    (catch Exception e nil))
            len (count ba)]
        (if (nil? ba)
            (http-not-found wtr)
            (do
              (.write wtr
                (str "HTTP/1.1 200 OK\r\n"
                     "Content-Type: text/html\r\n"
                     "Content-Length: " len "\r\n\r\n"))
              (.flush wtr)
              (.write out ba 0 len))))
      (let [mes (proc params in out rdr wtr)]
        (.write wtr
          (str "HTTP/1.1 200 OK\r\n"
               "Content-Type: application/json\r\n"
               "Content-Length: " (count mes) "\r\n\r\n"
               mes))
        (.flush wtr))))

(defn- http-proc-post [header body in out rdr wtr]
  (let [mes (proc body in out rdr wtr)]
    (.write wtr
      (str "HTTP/1.1 200 OK\r\n"
           "Content-Type: text/json\r\n"
           "Content-Length: " (count mes) "\r\n\r\n"
           mes))
    (.flush wtr)))

(defn- http-handler [in out]
  (with-open [rdr (reader in) wtr (writer out)]
    (loop [line (.readLine rdr) lines ""]
      (if (= line "")
          (if-let [header (http-header-parse (str lines))]
            (cond
              (= (header :method) "GET")
                (http-proc-get header in out rdr wtr)
              (= (header :method) "POST")
                (let [cl (Integer/parseInt
                           (:Content-Length
                           (:headers header)))
                      buf (make-array Byte/TYPE cl)
                      ; -> May be needed timeout
                      body (loop [len 0]
                             (let [n (.read in buf len (- cl len))]
                               (if (< n 0) ""
                                   (if (<= cl (+ len n))
                                       (apply str (map char buf))
                                       (recur (+ len n))))))]
                   (http-proc-post header body in out rdr wtr))
                :else (http-method-not-allowed wtr))
             (http-bad-request wtr)
          )
          (recur (.readLine rdr) (str lines line))))))

; ----------------------------------------------------------------

(def ^:dynamic *telnet-server* (atom nil))
(def ^:dynamic *http-server* (atom nil))

(defn- start-telnet-server [port]
  (reset! *telnet-server* (create-server port telnet-handler)))

(defn- stop-telnet-server []
  (when-not (nil? @*telnet-server*)
    (close-server @*telnet-server*)
    (reset! *telnet-server* nil)))

(defn- start-http-server [port]
  (reset! *http-server* (create-server port http-handler)))

(defn- stop-http-server []
  (when-not (nil? @*http-server*)
    (close-server @*http-server*)
    (reset! *http-server* nil)))

; ----------------------------------------------------------------

(defn -main [& args]
  (if-let [path (first args)]
    (ldb/cd path)
    (ldb/cd "."))
  (start-telnet-server 3000)
  (start-http-server 8080))

