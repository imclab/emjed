(ns emjed.ldb
  (:refer-clojure :exclude (load get set add-classpath))
  (:import [java.util Date]
           [clojure.lang DynamicClassLoader])
  (:require [cheshire.core :as json])
  (:use [clojure.java.io]
        [clojure.core.incubator :only [dissoc-in]]))

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

;; This is used to remove an URL in the classpath added with
;; p-add-classpath
(defn- init-classloader []
  (let [ccl (.getContextClassLoader (Thread/currentThread))
        dcl (DynamicClassLoader.
              (if (instance? DynamicClassLoader ccl) (.getParent ccl) ccl))]
    (.setContextClassLoader (Thread/currentThread) dcl)))

(defn- add-classpath [path]
  (let [cpath (.getCanonicalPath (file path))
        ccl (.getContextClassLoader (Thread/currentThread))
        url (as-url (str "file://" cpath "/"))]
    (if-not (instance? DynamicClassLoader ccl)
            (println "Do p-init-classloader before using p-add-classpath")
            (if-not (some #(.equals % url) (.getURLs ccl))
                    (.addURL ccl url)
                    (println "Already")))))

(defmacro save []
 `(doseq [[m# f#] [[@*conf* @*conf-file*] [@*prog* @*prog-file*]]]
    (spit (str @*dir* "/" f#)
      (json/generate-string m# {:pretty true}))))

(defn cd
  ([] (cd @*dir*))
  ([path]
    (do (if (= (first path) \/)
            (reset! *dir* path)
            (swap! *dir* #(.getCanonicalPath (file (str % "/" path))))))
        (init-classloader)
        (add-classpath (str @*dir* "/src"))
        (add-classpath (str @*dir* "/classes"))
        (load)))

; TODO import
; TODO export

