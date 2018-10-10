(ns clucie.utils)

(defn keyword->str
  ^String
  [x]
  (when (keyword? x)
    (if (some? (namespace x))
      (-> x str (subs 1))
      (name x))))
