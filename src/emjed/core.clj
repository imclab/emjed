(ns emjed.core
  (:gen-class)
  (:import  [java.io File]
            [java.net URL]
            [java.util Date]
            [clojure.lang DynamicClassLoader])
  (:use     [clojure.java.io]
            [server.socket])
  (:require [clojure.tools.logging :as log]
            [clj-json.core :as json]))

(def ^:dynamic *programs* (atom []))
(def ^:dynamic *runnings* (atom {}))

(defn- p-add-classpath [path]
  ; TODO if the path already registered, don't do this
  (let [cpath (.getCanonicalPath (File. path))
        ccl (.getContextClassLoader (Thread/currentThread))
        dcl (if (instance? DynamicClassLoader ccl) ccl
                (let [l (DynamicClassLoader. ccl)]
                  (.setContextClassLoader (Thread/currentThread) l)
                  l))]
    (.addURL dcl (URL. (str "file://" cpath "/")))))

(defn- p-require
  [p-name p-language p-main p-classes]
  (try
    (p-add-classpath p-classes)
    (require (symbol p-main))
    (swap! *programs* conj {:name p-name
                            :language p-language
                            :main p-main})
    "OK"
    (catch Exception e (.toString e))))

(defn- p-compile
  [p-name p-language p-main p-src p-classes & p-other-namespaces]
  (try
    (p-add-classpath p-src)
    (p-add-classpath p-classes)
    ; TODO if p-classes doesn't exist, create it
    (binding [*compile-path* (.getCanonicalPath (File. p-classes))]
      (doall (map #(compile (symbol %)) p-other-namespaces))
      (compile (symbol p-main))
    )
    (swap! *programs* conj {:name p-name
                            :language p-language
                            :main p-main})
    "OK"
    (catch Exception e (.toString e))))

(defn- exec [p-name & other-args]
  (let [p (first (filter #(= (:name %) p-name) @*programs*))
        m (if p (resolve (symbol (str (:main p) "/-main"))))]
    (if p
        (do
          (swap! *runnings*
            (fn [runnings]
              (let [pid (first
                          (drop-while
                            (fn [c] (some #(= c %) (keys runnings)))
                            (range)))]
                (assoc runnings pid {
                  :start-at (Date.)
                  :program p
                  :args other-args
                  :future (future (apply m other-args))}))))
          "OK")
          (str "No such program: " p-name))))

(defn- kill [str-pid]
  (try
    (let [pid (Integer/parseInt str-pid)]
      (swap! *runnings*
        (fn [runnings]
          (if-let [f (get-in runnings [pid :future])]
            (if-not (future-cancel f)
                    (throw (Exception. (str "Can't Stop Process: " pid))))
            (throw (Exception. (str "No Such Process: " pid))))
          (dissoc runnings pid)))
      "OK")
    (catch Exception e (.toString e))))

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

(defn- handler [in out]
  (with-open [rdr (reader in) wtr (writer out)]
    (loop [line (.readLine rdr)]
      (let [cmd-and-args (re-seq #"[^ \t\r\n]+" line)
            cmd (first cmd-and-args)
            args (rest cmd-and-args)]
        (when (not= cmd "close")
          (.write wtr
            (str
              (cond
                (= (first line) \`) (try (->> (rest line) (apply str)
                                           (load-string) (str))
                                         (catch Exception e (.toString e)))
                (= cmd "compile")  (apply p-compile args)
                (= cmd "require")  (apply p-require args)
                (= cmd "exec")     (apply exec args)
                (= cmd "kill")     (apply kill args)
                (= cmd "ps")       (ps)
                :else (str cmd ": command not found."))
              "\r\n"))
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
  (start))
