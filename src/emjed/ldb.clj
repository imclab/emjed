(ns emjed.ldb
  (:refer-clojure :exclude (load get set add-classpath))
  (:import [clojure.lang DynamicClassLoader])
  (:require [cheshire.core :as json])
  (:use [clojure.java.io]
        [clojure.core.incubator :only [dissoc-in]]))

(def ^:dynamic *dir* (atom (.getCanonicalPath (file "."))))
(def ^:dynamic *conf-file* (atom "conf.json"))
(def ^:dynamic *conf* (atom {}))
(def ^:dynamic *prog-file* (atom "prog.json"))
(def ^:dynamic *prog* (atom {}))

;; ----------------------------------------------------------------
;; general
(defmacro qk2kv [qk]
 `(vec (map keyword (re-seq #"[^:]+" ~qk))))

(defmacro kv2qk [kv]
 `(apply str (cons \: (interpose \: (map name ~kv)))))

(defmacro pwd [] `@*dir*)

(defmacro load []
 `(doseq [[a# f#] [[*conf* @*conf-file*] [*prog* @*prog-file*]]]
    (reset! a#
      (json/parse-string
        (try
          (slurp (str @*dir* "/" f#))
          (catch java.io.FileNotFoundException fnfe# "{}"))
        true)))) ; do convert strings to keywords

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
    (spit (str @*dir* "/" f#))
      (json/generate-string m# {:pretty true})))

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

;; ----------------------------------------------------------------
;; conf

(defmacro get [kv]
 `(if-let [r# (get-in @*conf* ~kv)]
    (if (map? r#) (keys r#) r#)
    (throw (Exception. (str "No Such Node: " (kv2qk ~kv))))))
      
(defmacro getrec [kv]
 `(if-let [r# (get-in @*conf* ~kv)]
    r#
    (throw (Exception. (str "No Such Node: " (kv2qk ~kv))))))

;; You can set the root as a plain value.
;; (set [] "hoge") -> "hoge"
;; (set [] {})     -> {}
;; (set [:foo] "hello") -> {...} -> {:foo "hello", ...}
;; (set [:foo :bar] "hell")
;;    {:foo {:bar "world", ...}, ...} -> {:foo {:bar "hell",  ...}, ...}
;;    {:foo {...}, ...} -> {:foo {:bar "hell", ...}, ...}
;;    {:foo "world", ...} -> Not A Holding Node: :foo
;;    {...} -> {:foo {:bar "hell"}, ...}
(defmacro set [kv v]
 `(if (empty? ~kv) (reset! *conf* ~v)
      (try (swap! *conf* assoc-in ~kv ~v)
           (catch ClassCastException cce#
             (throw (Exception. (str "Not A Holding Node: "
                                     (kv2qk (pop ~kv)))))))))

; Note:
; clojure.core.incubator/dissoc-in deletes ascendants if
; they comes an empty map by deleteing the specified key.
; For example
; (dissoc-in {:foo {:bar "hello"} :buz "world"} [:foo :bar]})
; -> {:buz "world"}
;
(defmacro del [kv]
 `(try (swap! *conf* dissoc-in ~kv)
       (catch ClassCastException cce#
         (throw (Exception. (str "Not A Holding Node: "
                                 (kv2qk (pop ~kv))))))))

(defmacro rename [skv dkv]
 `(try (swap! *conf* #(assoc-in (dissoc-in % ~skv) ~dkv (get-in % ~skv)))
       (catch ClassCastException cce#
         (throw (Exception. (str "Not A Holding Node: "))))))

;; ----------------------------------------------------------------
;; prog
;; libraries and executables.
;; An executable has an entry point function.
;; A name of entry point function should be -main
;;
; register
;   name
;   type
;   byte-length
;   source or jar
;  ?main
; src/air12/programs/?
;
; get source
;
; reload
;
; delete
;
; auto-exec?
; schedule?
;
; exec arbitrary function

;; ----------------------------------------------------------------
;; file
(defmacro get-cp [path]
 `(if (= (first ~path) \/) ~path (str @*dir* "/" ~path)))

(defmacro flist []
 `(->> ((fn l# [f#]
          (if (.isDirectory f#)
              (mapcat l# (.listFiles f#))
              (list (.getPath f#))))
        (file @*dir*))
    (map #(apply str (drop (inc (count @*dir*)) %)))))

(defmacro fput [path ba]
 `(with-open [os# (output-stream (get-cp ~path))]
    (.write os# ~ba)))

(defmacro fget [path]
 `(let [cp# (get-cp ~path)
        len# (.length (file cp#))
        ba# (byte-array len#)]
    (with-open [is# (input-stream cp#)]
      (.read is# ba# 0 len#))
    ba#))
 
(defmacro fdel [path]
 `(delete-file (get-cp ~path)))

(defmacro frename [spath dpath]
 `(.renameTo (file (get-cp ~spath)) (file (get-cp ~dpath))))
