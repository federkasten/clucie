(ns clucie.utils)

(defn keyword->str
  ^String
  [x]
  (if (some? (namespace x))
    (-> x str (subs 1))
    (name x)))
