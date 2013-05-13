(ns emjed.ldb
  (:refer-clojure :exclude (load get set add-classpath))
  (:import [java.util Date]
           [clojure.lang DynamicClassLoader])
  (:require [clojure.tools.logging :refer :all]
            [clojure.java.io :refer (file as-url input-stream output-stream
                                     reader writer delete-file)]
            [clojure.core.incubator :refer (dissoc-in)]
            [cheshire.core :as json]
            [overtone.at-at :as at-at]
            [emjed.utils :as utils]))

(def ^:dynamic *dir* (atom (.getCanonicalPath (file "."))))
(def ^:dynamic *conf-file* (atom "conf.json"))
(def ^:dynamic *conf* (atom {}))
(def ^:dynamic *prog-file* (atom "prog.json"))
(def ^:dynamic *prog* (atom {}))
(def ^:dynamic *file-dir* (atom "files"))

(clojure.core/load "ldb_conf")
(clojure.core/load "ldb_prog")
(clojure.core/load "ldb_file")

;; ----------------------------------------------------------------
;; general
;(eval-when-compile
(defmacro qk2kv [qk]
 `(vec (map keyword (re-seq #"[^:]+" ~qk))))

(defmacro kv2qk [kv]
 `(apply str (cons \: (interpose \: (map name ~kv)))))

(defmacro pwd [] `@*dir*)

(defmacro load []
 `(do
    (doseq [[a# f#] [[*conf* @*conf-file*] [*prog* @*prog-file*]]]
      (reset! a#
        (json/parse-string
          (try
            (slurp (str @*dir* "/" f#))
            (catch java.io.FileNotFoundException fnfe# "{}"))
          true))) ; do convert strings to keywords
    (doseq [[p-name-kw# {exec# :execution}] @*prog*]
      (pload p-name-kw#)
      (if (= exec# "AUTO")
          (exec p-name-kw#)))))
;)

;; This is used to remove an URL in the classpath added with
;; p-add-classpath
(defn- init-classloader []
  (let [ccl (.getContextClassLoader (Thread/currentThread))]
    (if-not (instance? DynamicClassLoader ccl)
            (.setContextClassLoader
              (Thread/currentThread)
              (DynamicClassLoader. ccl)))))

(defn- add-classpath [path]
  (let [ccl (.getContextClassLoader (Thread/currentThread))]
    (if-not (instance? DynamicClassLoader ccl)
            (fatal "Do init-classloader before using p-add-classpath")
            (let [url (.toURL (.toURI (file path)))]
              (if-not (some #(.equals ^java.net.URL % url)
                             (.getURLs ^DynamicClassLoader ccl))
                      (.addURL ^DynamicClassLoader ccl url)
                      (warn "The URL " url " is already in the CLASSPATH.")
                      )))))

;(eval-when-compile
(defmacro save []
 `(doseq [[m# f#] [[@*conf* @*conf-file*] [@*prog* @*prog-file*]]]
    (spit (str @*dir* "/" f#)
      (json/generate-string m# {:pretty true}))))
;)

(defn cd
  ([] (cd @*dir*))
  ([path]
    (do (if (= (first path) \/)
            (reset! *dir* path)
            (swap! *dir* #(.getCanonicalPath (file (str % "/" path))))))
        (init-classloader)
        (add-classpath (str @*dir* "/src"))
        (add-classpath (str @*dir* "/classes"))
        (trace
          (map identity
               (.getURLs (.getContextClassLoader (Thread/currentThread)))))
        (load)))

; TODO import
; TODO export

