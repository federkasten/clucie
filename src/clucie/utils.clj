(ns clucie.utils)

(defn keyword->str
  ^String
  [x]
  (if (some? (namespace x))
    (-> x str (subs 1))
    (name x)))

(defn stringify-value
  ^String
  [v]
  (when (some? v)
    (cond
      (string? v) v
      (keyword? v) (keyword->str v)
      :else (str v))))