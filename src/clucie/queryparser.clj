(ns clucie.queryparser
  (:import [org.apache.lucene.queryparser.classic QueryParser]
           [org.apache.lucene.analysis Analyzer]
           [org.apache.lucene.search Query]))

(defn ^Query parse-query
  [^Analyzer analyzer ^String default-field-name ^String query-string]
  (let [^QueryParser qp (QueryParser. default-field-name analyzer)
        _ (.setAutoGeneratePhraseQueries qp true)]
    (.parse qp query-string)))
