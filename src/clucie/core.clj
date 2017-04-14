(ns clucie.core
  (:require [clucie.store :as store]
            [clucie.analysis :refer [standard-analyzer]])
  (:import [org.apache.lucene.document Document Field FieldType]
           [org.apache.lucene.util QueryBuilder]
           [org.apache.lucene.index IndexWriter IndexReader IndexOptions Term]
           [org.apache.lucene.search BooleanClause BooleanClause$Occur BooleanQuery IndexSearcher Query PhraseQuery PhraseQuery$Builder WildcardQuery ScoreDoc TopDocs]))

(defn- estimate-value
  [v]
  (cond
    (string? v) {:value v :value-type :string}
    (integer? v) {:value (str v) :value-type :integer}
    (keyword? v) {:value (name v) :value-type :keyword}
    :else {:value (str v) :value-type :unknown}))

(defn- stringify-value
  ^String
  [v]
  (cond
    (string? v) v
    (integer? v) (str v)
    (keyword? v) (name v)
    :else (str v)))

(defn- gen-field-type
  ^FieldType
  [indexed?]
  (let [field-type (doto (new FieldType)
                     (.setStored true))]
    (if indexed?
      (doto field-type
        (.setIndexOptions IndexOptions/DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS)
        (.setTokenized true))
      (doto field-type
        (.setIndexOptions IndexOptions/NONE)
        (.setTokenized false)))
    field-type))

(defn- add-field
  "Add a Field to a Document."
  [^Document document key value & [indexed?]]
  (let [{:keys [^String value value-type]} (estimate-value value)
        ^String key (name key)
        field (Field. key value (gen-field-type indexed?))]
    (.add document field)))

(defn- map->document
  "Create a Document from a map."
  [m keys]
  (let [document (Document.)]
    (doseq [[key value] m]
      (add-field document key value (contains? keys key)))
    document))

(defn add!
  "Add hash-maps to the search index."
  ([index-store maps keys]
   (add! index-store maps keys (standard-analyzer)))
  ([index-store maps keys analyzer]
   (with-open [writer (store/store-writer index-store analyzer)]
     (doseq [m maps]
       (.addDocument writer
                     (map->document m (set keys)))))))

(defn update!
  ([index-store m keys search-key search-val]
   (update! index-store m keys search-key search-val (standard-analyzer)))
  ([index-store m keys search-key search-val analyzer]
   (with-open [writer (store/store-writer index-store analyzer)]
     (.updateDocument writer
                      (Term. (name search-key) (stringify-value search-val))
                      (map->document m (set keys)))
     nil)))

(defn delete!
  ([index-store search-key search-val]
   (delete! index-store search-key search-val (standard-analyzer)))
  ([index-store search-key search-val analyzer]
   (with-open [writer (store/store-writer index-store analyzer)]
     (.deleteDocuments writer
                       ^"[Lorg.apache.lucene.index.Term;"
                       (into-array [(Term. (name search-key) (stringify-value search-val))]))
     nil)))

(defn- document->map
  "Turn a Document object into a map."
  [^Document document]
  (into {} (for [^Field f (.getFields document)]
             [(keyword (.name f)) (.stringValue f)])))

(defn- query-form->query
  [mode query-form ^QueryBuilder builder & {:keys [current-key]
                                       :or {current-key nil}}]
  (cond
    (sequential? query-form) (let [qb (new org.apache.lucene.search.BooleanQuery$Builder)]
                               (doseq [q (map #(query-form->query mode % builder :current-key current-key) query-form)]
                                 (when q
                                   (.add qb q BooleanClause$Occur/MUST)))
                               (.build qb))
    (set? query-form) (let [qb (new org.apache.lucene.search.BooleanQuery$Builder)]
                        (doseq [q (map #(query-form->query mode % builder :current-key current-key) query-form)]
                          (when q
                            (.add qb q BooleanClause$Occur/SHOULD)))
                        (.build qb))
    (map? query-form) (let [qb (new org.apache.lucene.search.BooleanQuery$Builder)]
                        (doseq [q (->> query-form
                                       (map (fn [[k v]]
                                              (query-form->query mode v builder :current-key k)))
                                       (filter identity))]
                          (.add qb q BooleanClause$Occur/MUST))
                        (.build qb))
    (string? query-form) (case mode
                           :query (.createBooleanQuery builder (name current-key) query-form)
                           :phrase-query (.createPhraseQuery builder (name current-key) query-form)
                           :wildcard-query (WildcardQuery. (Term. (name current-key) (str query-form)))
                           (throw (ex-info "invalid mode" {:mode mode})))))

(defn- search*
  [mode index-store query-form max-results analyzer page results-per-page]
  (with-open [reader (store/store-reader index-store)]
    (let [analyzer (or analyzer (standard-analyzer))
          page (or page 0)
          results-per-page (or results-per-page max-results)
          ^IndexSearcher searcher (IndexSearcher. reader)
          builder (QueryBuilder. analyzer)
          ^BooleanQuery query (query-form->query mode query-form builder)
          ^TopDocs hits (.search searcher query (int max-results))
          start (* page results-per-page)
          end (min (+ start results-per-page) (.totalHits hits) max-results)]
      (vec
        (for [^ScoreDoc hit (map (partial aget (.scoreDocs hits))
                                 (range start end))]
          (let [m (document->map (.doc searcher (.doc hit)))
                score (.score hit)]
            (with-meta m {:score score})))))))

(defn search
  "Search the supplied index with a query string."
  [index-store query-form max-results & [analyzer page results-per-page]]
  (search* :query index-store query-form max-results analyzer page results-per-page))

(defn phrase-search
  "Phrase-search the supplied index with a query string."
  [index-store query-form max-results & [analyzer page results-per-page]]
  (search* :phrase-query index-store query-form max-results analyzer page results-per-page))

(defn wildcard-search
  "Wildcard-search the supplied index with a query string."
  [index-store query-form max-results & [analyzer page results-per-page]]
  (search* :wildcard-query index-store query-form max-results analyzer page results-per-page))
