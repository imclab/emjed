(in-ns 'emjed.ldb)

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
