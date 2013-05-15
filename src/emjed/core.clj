(ns emjed.core
  (:gen-class)
  (:require [server.socket :refer (create-server close-server)]
            [clojure.java.io :refer (reader writer resource)]
            [clojure.string :as cs]
            [clojure.tools.logging :refer :all]
            [clojure.data.json :as json]
            [emjed.ldb :as ldb]
            [emjed.mutex :as mutex]))

;; ----------------------------------------------------------------
;; general
;;
(defn get-version []
  (let [sdf (java.text.SimpleDateFormat. "yyyyMMddHHmmss")]
    (->>
      (resource "emjed/core__init.class")
      (.openConnection)
      (.getLastModified)
      (#(.format sdf ^java.util.Date %)))))

;; ----------------------------------------------------------------
;; handling

(defmacro jsonize [body]
 `(json/write-str ~body))
; For, now I gave prity-print, so that the code below is very slow...
;`(cs/replace
;   (with-out-str (json/pprint ~body) (flush))
;   #"\n" "\r\n")

(defmacro unjson [body]
 `(json/read-str ~body :key-fn keyword))

(defmacro bs-apply [s]
 `(loop [c# ~s]
    (let [r# (cs/join (cs/split c# #"[^\u0008]\u0008"))]
      (if (= r# c#) r# (recur r#)))))

(defn- ^java.lang.String proc
  [line ^java.io.BufferedReader rdr ^java.io.BufferedWriter wtr]
  (str
    (let [splits (re-seq #"[^ \t\r\n]+" line)
          cmd  (first splits)
          args (rest splits)]
      (try
        (if (= (first line) \`)
            ; escape
            (->> (rest line) (apply str) (load-string) (str))
            (condp = cmd
              ; general
              "version"    (str "emjed-" (get-version))
              "pwd"        (ldb/pwd)
              "cd"         (do (ldb/cd (first args)) "OK")
              "load"       (do (ldb/load) "OK")
              "save"       (do (ldb/save) "OK")
             ;"export"
             ;"import"
              ; libraries and programs
              "register"   (do (warn "register: " (apply str (rest args)))
                               ; trace
                               (ldb/register (keyword (first args))
                                             (unjson
                                               (apply str (rest args))))
                               "OK")
              "pload"      (do (ldb/pload (keyword (first args))) "OK")
              "registered" (jsonize (ldb/registered))
              "unregister" (do (ldb/unregister (keyword (first args))) "OK")
              "build"      (do (ldb/build (keyword (first args))) "OK")
              "exec-fn"    (ldb/exec-fn (first args) (rest args))
              "exec"       (ldb/exec (keyword (first args)))
              "kill"       (do (ldb/kill (Integer/parseInt (first args)))
                               "OK")
              "ps"         (jsonize (ldb/ps))
              ; ldb conf
              "get"        (jsonize (ldb/get (ldb/qk2kv (first args))))
              "getrec"     (jsonize (ldb/getrec (ldb/qk2kv (first args))))
              "set"        (do (ldb/set (ldb/qk2kv (first args))
                                        (unjson (second args)))
                               "OK")
              "del"        (do (ldb/del (ldb/qk2kv (first args))) "OK")
              "rename"     (do (ldb/rename (ldb/qk2kv (first args))
                                            (ldb/qk2kv (second args)))
                               "OK")
              ; ldb file
              "flist"      (jsonize (ldb/flist))
              "fget"       (let [ca (ldb/fget (first args))
                                 len (count ca)]
                             (.write wtr (str len "\r\n"))
                             (.flush wtr)
                             (.write wtr ^chars ca 0 len)
                             "")
              "ftget"      (apply str
                             (interleave
                               (re-seq #"[^\r\n]+"
                                 (slurp (str (ldb/pwd) "/files/"
                                             (first args))))
                               (repeat "\r\n")))
              "fput"       (let [len (Integer/parseInt (second args))
                                 ca (char-array len)]
                             ; TODO loop with a timeout
                             (.read rdr ^chars ca 0 len)
                             (.readLine rdr)
                             (ldb/fput (first args) ca)
                             "OK")
              "ftput"      (let [fname (str (ldb/pwd) "/files/" (first args))
                                 emark (second args)]
                             (loop [line (.readLine rdr) lines ""]
                               (if (or (= line emark)
                                       (= (int (first line)) 0x04))
                                   (spit fname lines)
                                   (recur (.readLine rdr)
                                          (str lines "\r\n" line))))
                             "OK")
              "fdel"       (do (ldb/fdel (first args)) "OK")
              "frename"    (do (ldb/frename (first args) (second args)) "OK")
              (str cmd ": command not found.")))
        (catch Throwable t (.toString t))))
    "\r\n"))

;; ----------------------------------------------------------------
(defn- telnet-handler [in out]
  (try
    (with-open [^java.io.BufferedReader rdr (reader in)
                ^java.io.BufferedWriter wtr (writer out)]
      (loop [line (bs-apply (.readLine rdr))]
        (if (= (apply str (take 5 line)) "close")
            nil
            (do (.write wtr (proc line rdr wtr))
                (.flush wtr)
                (recur (bs-apply (.readLine rdr)))))))
    (catch Throwable t (.printStackTrace t))))

;; ----------------------------------------------------------------
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
    (let [[firstline & headers]    (cs/split lines #"\r\n")
          [[_ method _ proto host _ port path _ params httpv]]
            (re-seq
  #"(\S+)\s+(([^:\s]+)://)?([^/:\s]*)(:([^/\s]*))?([^\?\s]+)(\?(\S+))?\s+(\S+)"
  ; method  .proto         host      . port       path      .  params    ver
              firstline)]
      {:method method :proto proto :host host :port port :path path
       :httpv httpv
       :headers (if (nil? headers) nil
                    (into {} (for [p headers
                                   :let [[s v] (cs/split p #": *")]]
                               [(keyword s) v])))
       :params (decode-url-encoded params)
       })
    (catch Throwable t (.printStackTrace t) nil)))

(defn- http-method-not-allowed [^java.io.BufferedWriter wtr]
  (let [mes "<html><body><h1>405 Method Not Allowed</h1></body></html>"]
    (.write wtr
      (str "HTTP/1.1 405 Method Not Allowed\r\n"
           "Access-Control-Allow-Origin: *\r\n"
           "Content-Length: " (+ 2 (count mes)) "\r\n\r\n"
           mes "\r\n"))
    (.flush wtr)))

(defn- http-bad-request [^java.io.BufferedWriter wtr]
  (let [mes "<html><body><h1>400 Bad Request</h1></body></html>"]
    (.write wtr
      (str "HTTP/1.1 400 Bad Request\r\n"
           "Access-Control-Allow-Origin: *\r\n"
           "Content-Length: " (+ 2 (count mes)) "\r\n\r\n"
           mes "\r\n"))
    (.flush wtr)))

(defn- http-not-found [^java.io.BufferedWriter wtr]
  (let [mes "<html><body><h1>404 Not Found</h1></body></html>"]
    (.write wtr
      (str "HTTP/1.1 404 Not Found\r\n"
           "Access-Control-Allow-Origin: *\r\n"
           "Content-Length: " (+ 2 (count mes)) "\r\n\r\n"
           mes "\r\n"))
    (.flush wtr)))

(defn- http-proc-get
  [{path :path params :params}
   ^java.io.BufferedReader rdr
   ^java.io.BufferedWriter wtr]
  (if (not= path "/")
      (let [ca (try (ldb/fget (apply str (rest path)))
                    (catch Exception e nil)
                    (catch Error e (do (.printStackTrace e) nil)))
            len (count ca)]
        (if (nil? ca)
            (http-not-found wtr)
            (do
              (.write wtr
                (str "HTTP/1.1 200 OK\r\n"
                     "Access-Control-Allow-Origin: *\r\n"
                     "Content-Type: text/html\r\n"
                     "Content-Length: " len "\r\n\r\n"))
              (.flush wtr)
              (.write wtr ^chars ca 0 len))))
      (let [mes (proc params rdr wtr)]
        (.write wtr
          (str "HTTP/1.1 200 OK\r\n"
               "Access-Control-Allow-Origin: *\r\n"
               "Content-Type: application/json\r\n"
               "Content-Length: " (count mes) "\r\n\r\n"
               mes))
        (.flush wtr))))

(defn- http-proc-post
  [header body ^java.io.BufferedReader rdr ^java.io.BufferedWriter wtr]
  (let [mes (proc body rdr wtr)]
    (.write wtr
      (str "HTTP/1.1 200 OK\r\n"
           "Access-Control-Allow-Origin: *\r\n"
           "Content-Type: text/json\r\n"
           "Content-Length: " (count mes) "\r\n\r\n"
           mes))
    (.flush wtr)))

(defn- http-handler [in out]
  (try
    (with-open [^java.io.BufferedReader rdr (reader in)
                ^java.io.BufferedWriter wtr (writer out)]
      (loop [line (.readLine rdr) lines ""]
        (if (= line "")
            (if-let [header (http-header-parse (str lines))]
              (condp = (header :method)
                "GET"  (http-proc-get header rdr wtr)
                "POST" (let [cl (Integer/parseInt
                                  (:Content-Length (:headers header)))
                             buf (char-array cl)
                             ; -> May be needed timeout
                             body (loop [len 0]
                                    (let [n (.read rdr ^chars buf
                                                   len (- cl len))]
                                      (if (< n 0) ""
                                          (if (<= cl (+ len n))
                                              (apply str (map char buf))
                                              (recur (+ len n))))))]
                         (http-proc-post header
                           (decode-url-encoded body) rdr wtr))
                (http-method-not-allowed wtr))
              (http-bad-request wtr))
            (recur (.readLine rdr) (str lines line "\r\n")))))
    (catch Throwable t (.printStackTrace t))))

;; ----------------------------------------------------------------

(def telnet-server (atom nil))
(def http-server   (atom nil))

(defn- start-telnet-server [port]
  (reset! telnet-server (create-server port telnet-handler)))

(defn- stop-telnet-server []
  (when-not (nil? @telnet-server)
    (close-server @telnet-server)
    (reset! telnet-server nil)))

(defn- start-http-server [port]
  (reset! http-server (create-server port http-handler)))

(defn- stop-http-server []
  (when-not (nil? @http-server)
    (close-server @http-server)
    (reset! http-server nil)))

;; ----------------------------------------------------------------

(defn -main [& args]
  (if-let [path (first args)]
    (ldb/cd path)
    (ldb/cd "."))
  (start-telnet-server 3000)
  (start-http-server 8080))

