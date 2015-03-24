(ns clucie.core
  (:require [clucie.store :as store])
  (:import [org.apache.lucene.document Document Field FieldType]
           [org.apache.lucene.analysis.standard StandardAnalyzer]
           [org.apache.lucene.queryparser.classic QueryParser]
           [org.apache.lucene.index IndexReader IndexOptions]
           [org.apache.lucene.search BooleanClause BooleanClause$Occur BooleanQuery IndexSearcher Query ScoreDoc Scorer TermQuery]))

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

(defn- add-indexed-field
  "Add a Field to a Document."
  [document key value]
  (let [{:keys [value value-type]} (estimate-value value)]
    (.add ^Document document
          (Field. (name key) value (gen-field-type true)))))

(defn- add-field
  "Add a Field to a Document."
  [document key value]
  (let [{:keys [value value-type]} (estimate-value value)]
    (.add ^Document document
          (Field. (name key) value (gen-field-type false)))))

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
  [index-store maps keys]
  (with-open [writer (store/store-writer index-store)]
    (doseq [m maps]
      (.addDocument writer
                    (map->document m (set keys))))))

(defn- document->map
  "Turn a Document object into a map."
  [^Document document]
  (into {} (for [^Field f (.getFields document)]
             [(keyword (.name f)) (.stringValue f)])))

(defn search
  "Search the supplied index with a query string."
  [index-store key query-string max-results]
  (with-open [reader (store/store-reader index-store)]
    (let [searcher (IndexSearcher. reader)
          parser (doto (QueryParser. (name key) (StandardAnalyzer.))
                   (.setDefaultOperator QueryParser/AND_OPERATOR))
          query (.parse parser query-string)
          hits (.search searcher query (int max-results))]
      (doall
       (for [hit (map (partial aget (.scoreDocs hits))
                      (range (.totalHits hits)))]
         (document->map (.doc ^IndexSearcher searcher (.doc ^ScoreDoc hit))))))))
