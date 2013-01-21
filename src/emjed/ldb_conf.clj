(in-ns 'emjed.ldb)

;(eval-when-compile
(defmacro get [kv]
 `(if-let [r# (get-in @*conf* ~kv)]
    (if (map? r#) (keys r#) r#)))
      
(defmacro getrec [kv]
 `(if-let [r# (get-in @*conf* ~kv)]
    r#
    {}))

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

;)
