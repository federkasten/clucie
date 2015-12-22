(ns clucie.core
  (:require [clucie.store :as store]
            [clucie.analysis :refer [standard-analyzer]])
  (:import [org.apache.lucene.document Document Field FieldType]
           [org.apache.lucene.util QueryBuilder]
           [org.apache.lucene.analysis Analyzer]
           [org.apache.lucene.index IndexWriter IndexReader IndexOptions Term]
           [org.apache.lucene.search BooleanClause BooleanClause$Occur BooleanQuery IndexSearcher Query ScoreDoc TopDocs]))

(defn- estimate-value
  [v]
  (cond
    (string? v) {:value v :value-type :string}
    (integer? v) {:value (str v) :value-type :integer}
    (keyword? v) {:value (name v) :value-type :keyword}
    :else {:value (str v) :value-type :unknown}))

(defn- gen-field-type
  ^FieldType
  [indexed?]
  (let [field-type (doto (new FieldType)
                     (.setStored true))]
    (if indexed?
      (doto field-type
        (.setIndexOptions IndexOptions/DOCS)
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

(defn- add-indexed-field
  "Add a Field to a Document."
  [^Document document key value]
  (add-field document key value true))

(defn- map->document
  "Create a Document from a map."
  [m keys]
  (let [document (Document.)]
    (doseq [[key value] m]
      ((if (contains? keys key)
          add-indexed-field
          add-field)
       document key value))
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
                      (Term. (name search-key) (str search-val))
                      (map->document m (set keys))))))

(defn delete!
  ([index-store search-key search-val]
   (delete! index-store search-key search-val (standard-analyzer)))
  ([index-store search-key search-val analyzer]
   (with-open [writer (store/store-writer index-store analyzer)]
     (.deleteDocuments writer
                       ^"[Lorg.apache.lucene.index.Term;"
                       (into-array [(Term. (name search-key) (str search-val))])))))

(defn- document->map
  "Turn a Document object into a map."
  [^Document document]
  (into {} (for [^Field f (.getFields document)]
             [(keyword (.name f)) (.stringValue f)])))

(defn- query-form->query
  [query-form ^QueryBuilder builder]
  (cond
    (vector? query-form) (let [query (BooleanQuery.)]
                           (doseq [q (map #(query-form->query % builder) query-form)]
                             (.add query q BooleanClause$Occur/SHOULD))
                           query)
    (map? query-form) (let [query (BooleanQuery.)]
                        (doseq [q (->>  query-form
                                        (map (fn [[k v]]
                                               (when-not (empty? (str v))
                                                 (.createBooleanQuery builder (name k) (str v)))))
                                        (filter #(not (nil? %))))]
                          (.add query q BooleanClause$Occur/MUST))
                        query)))

(defn search
  "Search the supplied index with a query string."
  [index-store query-form max-results & [analyzer page results-per-page]]
  (with-open [reader (store/store-reader index-store)]
    (let [analyzer (or analyzer (standard-analyzer))
          page (or page 0)
          results-per-page (or results-per-page max-results)
          ^IndexSearcher searcher (IndexSearcher. reader)
          builder (QueryBuilder. analyzer)
          ^BooleanQuery query (query-form->query query-form builder)
          ^TopDocs hits (.search searcher query (int max-results))
          start (* page results-per-page)
          end (min (+ start results-per-page) (.totalHits hits) max-results)]
      (vec
        (for [^ScoreDoc hit (map (partial aget (.scoreDocs hits))
                                 (range start end))]
          (document->map (.doc searcher (.doc hit))))))))
