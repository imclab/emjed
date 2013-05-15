(ns emjed.ldb
  (:refer-clojure :exclude (load get set add-classpath))
  (:import [java.util Date]
           [clojure.lang DynamicClassLoader])
  (:require [clojure.tools.logging :refer :all]
            [clojure.java.io :refer (file as-url input-stream output-stream
                                     reader writer delete-file)]
            [clojure.core.incubator :refer (dissoc-in)]
            [clojure.data.json :as json]
            [overtone.at-at :as at-at]))

(def dir (atom (.getCanonicalPath (file "."))))
(def conf-file (atom "conf.json"))
(def conf (atom {}))
(def prog-file (atom "prog.json"))
(def prog (atom {}))
(def file-dir (atom "files"))

(clojure.core/load "ldb_conf")
(clojure.core/load "ldb_prog")
(clojure.core/load "ldb_file")

;; ----------------------------------------------------------------
;; general
(defmacro qk2kv [qk]
 `(vec (map keyword (re-seq #"[^:]+" ~qk))))

(defmacro kv2qk [kv]
 `(apply str (cons \: (interpose \: (map name ~kv)))))

(defmacro pwd [] `@dir)

(defmacro load []
 `(do
    (doseq [[a# f#] [[conf @conf-file] [prog @prog-file]]]
      (reset! a#
        (json/read-str
          (try
            (slurp (str @dir "/" f#))
            (catch java.io.FileNotFoundException fnfe# "{}"))
          :key-fn keyword))) ; do convert strings to keywords
    (doseq [[p-name-kw# {exec# :execution}] @prog]
      (pload p-name-kw#)
      (if (= exec# "AUTO")
          (exec p-name-kw#)))))

;; This is used to remove an URL in the classpath added with
;; add-classpath
(defn- init-classloader []
  (let [ccl (.getContextClassLoader (Thread/currentThread))]
    (if-not (instance? DynamicClassLoader ccl)
            (.setContextClassLoader
              (Thread/currentThread)
              (DynamicClassLoader. ccl)))))

(defn- add-classpath [path]
  (let [ccl (.getContextClassLoader (Thread/currentThread))]
    (if-not (instance? DynamicClassLoader ccl)
            (fatal "Do init-classloader before using add-classpath")
            (let [url (.toURL (.toURI (file path)))]
              (if-not (some #(.equals ^java.net.URL % url)
                             (.getURLs ^DynamicClassLoader ccl))
                      (.addURL ^DynamicClassLoader ccl url)
                      (warn "The URL " url " is already in the CLASSPATH.")
                      )))))

(defmacro save []
 `(doseq [[m# f#] [[@conf @conf-file] [@prog @prog-file]]]
    (spit (str @dir "/" f#)
      (.replace (with-out-str (json/pprint m# )) "\n" "\r\n"))))

(defn cd
  ([] (cd @dir))
  ([path]
    (do (if (= (first path) \/)
            (reset! dir path)
            (swap! dir #(.getCanonicalPath (file (str % "/" path))))))
        (init-classloader)
        (add-classpath (str @dir "/src"))
        (add-classpath (str @dir "/classes"))
        (trace
          (map identity
               (.getURLs ^DynamicClassLoader
                         (.getContextClassLoader (Thread/currentThread)))))
        (load)))

; TODO import
; TODO export

