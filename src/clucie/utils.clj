(ns clucie.utils)

(defn keyword-or-symbol? [x]
  (or (keyword? x)
      (symbol? x)))

(defn qualified? [x]
  (boolean (and (keyword-or-symbol? x)
                (namespace x)
                true)))

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
