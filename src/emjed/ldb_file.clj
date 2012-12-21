(in-ns 'emjed.ldb)

;(eval-when-compile
(defmacro get-cp [path]
 `(if (= (first ~path) \/) ~path (str @*dir* "/" @*file-dir* "/" ~path)))

(defmacro flist []
 `(let [cp# (str @*dir* "/" @*file-dir*)]
    (->> ((fn l# [f#]
            (if (.isDirectory f#)
                (mapcat l# (.listFiles f#))
                (list (.getPath f#))))
          (file cp#))
      (map #(apply str (drop (inc (count cp#)) %))))))

(defmacro fput [path ca]
 `(with-open [os# (writer (output-stream (get-cp ~path)))]
    (.write os# ~ca)))

(defmacro fget [path]
 `(let [cp# (get-cp ~path)
        len# (.length (file cp#))
        ca# (char-array len#)]
    (with-open [is# (reader (input-stream cp#))]
      (.read is# ca# 0 len#))
    ca#))
 
(defmacro fdel [path]
 `(delete-file (get-cp ~path)))

(defmacro frename [spath dpath]
 `(.renameTo (file (get-cp ~spath)) (file (get-cp ~dpath))))
;)
