(ns clucie.core
  (:require [clucie.store :as store]
            [clucie.analysis :refer [standard-analyzer]]
            [clucie.utils :refer [stringify-value]]
            [clucie.document :as doc]
            [clucie.query :as q])
  (:import [org.apache.lucene.index IndexWriter IndexReader IndexOptions Term]
           [org.apache.lucene.search IndexSearcher ScoreDoc TopDocs]
           [org.apache.lucene.store Directory]))

(defmulti add!
  "Adds documents represented as maps to the search index."
  {:arglists '([store-or-writer maps keys]
               [store maps keys analyzer])}
  #(class (first %&)))

(defmethod add! Directory
  ([index-store maps keys]
   (add! index-store maps keys (standard-analyzer)))
  ([index-store maps keys analyzer]
   (with-open [writer (store/store-writer index-store analyzer)]
     (add! writer maps keys))))

(defmethod add! IndexWriter
  [^IndexWriter writer maps keys]
  (doseq [m maps]
    (.addDocument writer (doc/document m (set keys)))))

(defmulti update!
  "Updates a document containing search-val on search-key."
  {:arglists '([store-or-writer m keys search-key search-val]
               [store m keys search-key search-val analyzer])}
  #(class (first %&)))

(defmethod update! Directory
  ([index-store m keys search-key search-val]
   (update! index-store m keys search-key search-val (standard-analyzer)))
  ([index-store m keys search-key search-val analyzer]
   (with-open [writer (store/store-writer index-store analyzer)]
     (update! writer m keys search-key search-val))))

(defmethod update! IndexWriter
  [^IndexWriter writer m keys search-key search-val]
  (.updateDocument writer
                   (Term. (stringify-value search-key) (stringify-value search-val))
                   (doc/document m (set keys)))
  nil)

(defmulti delete!
  "Deletes the document containing search-val on search-key."
  {:arglists '([store-or-writer search-key search-val]
               [store search-key search-val analyzer])}
  #(class (first %&)))

(defmethod delete! Directory
  ([index-store search-key search-val]
   (delete! index-store search-key search-val (standard-analyzer)))
  ([index-store search-key search-val analyzer]
   (with-open [writer (store/store-writer index-store analyzer)]
     (delete! writer search-key search-val))))

(defmethod delete! IndexWriter
  [^IndexWriter writer search-key search-val]
  (.deleteDocuments writer
                    ^"[Lorg.apache.lucene.index.Term;"
                    (into-array [(Term. (stringify-value search-key) (stringify-value search-val))]))
  nil)

(defmulti ^:private search* #(class (second %&)))

(defmethod search* Directory
  [mode index-store query-form max-results analyzer page results-per-page]
  (with-open [reader (store/store-reader index-store)]
    (search* mode reader query-form max-results analyzer page results-per-page)))

(defmethod search* IndexReader
  [mode ^IndexReader reader query-form max-results analyzer page results-per-page]
  (let [analyzer (or analyzer (standard-analyzer))
        page (or page 0)
        results-per-page (or results-per-page max-results)
        ^IndexSearcher searcher (IndexSearcher. reader)
        query (q/parse-form query-form :analyzer analyzer :mode mode)
        ^TopDocs hits (.search searcher query (int max-results))
        start (* page results-per-page)
        end (min (+ start results-per-page) (.totalHits hits) max-results)]
    (with-meta
      (vec
       (for [^ScoreDoc hit (map (partial aget (.scoreDocs hits))
                                (range start end))]
         (let [doc-id (.doc hit)
               m (doc/document->map (.doc searcher doc-id))
               score (.score hit)]
           (with-meta m {:score score :doc-id doc-id}))))
      {:total-hits (.totalHits hits)})))

(defn search
  "Search the supplied index with a query string."
  [store-or-reader query-form max-results & [analyzer page results-per-page]]
  (search* :query store-or-reader query-form max-results analyzer page results-per-page))

(defn phrase-search
  "Search the supplied index with a pharse query string."
  [store-or-reader query-form max-results & [analyzer page results-per-page]]
  (search* :phrase-query store-or-reader query-form max-results analyzer page results-per-page))

(defn wildcard-search
  "Search the supplied index with a wildcard query string."
  [store-or-reader query-form max-results & [analyzer page results-per-page]]
  (search* :wildcard-query store-or-reader query-form max-results analyzer page results-per-page))

(defn qp-search
  "Search the supplied index with a classic-queryparser query string.
  NB: This may throw org.apache.lucene.queryparser.classic.ParseException
  by invalid query string."
  [store-or-reader query-form max-results & [analyzer page results-per-page]]
  (search* :qp-query store-or-reader query-form max-results analyzer page results-per-page))
