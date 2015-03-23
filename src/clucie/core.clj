(ns clucie.core
  (:require [clucie.store :as store])
  (:import [org.apache.lucene.document Document Field Field$Index Field$Store]
           [org.apache.lucene.analysis.standard StandardAnalyzer]
           [org.apache.lucene.queryparser.classic QueryParser]
           [org.apache.lucene.index IndexReader]
           [org.apache.lucene.search BooleanClause BooleanClause$Occur BooleanQuery IndexSearcher Query ScoreDoc Scorer TermQuery]))

(defn- add-field
  "Add a Field to a Document."
  [document key value]
  (.add ^Document document
        (Field. (name key) (str value)
                Field$Store/YES
                Field$Index/ANALYZED)))

(defn- map->document
  "Create a Document from a map."
  [map]
  (let [document (Document.)]
    (doseq [[key value] map]
      (add-field document key value))
    document))

(defn add!
  "Add hash-maps to the search index."
  [index-store & maps]
  (with-open [writer (store/store-writer index-store)]
    (doseq [m maps]
      (.addDocument writer
                    (map->document m)))))

(defn- document->map
  "Turn a Document object into a map."
  [^Document document]
  (into {} (for [^Field f (.getFields document)]
             [(keyword (.name f)) (.stringValue f)])))

(defn search
  "Search the supplied index with a query string."
  [index-store query key max-results]
  (with-open [reader (store/store-reader index-store)]
    (let [searcher (IndexSearcher. reader)
          parser (doto (QueryParser. (name key) (StandardAnalyzer.))
                   (.setDefaultOperator QueryParser/AND_OPERATOR))
          query (.parse parser query)
          hits (.search searcher query (int max-results))]
      (doall
       (for [hit (map (partial aget (.scoreDocs hits))
                      (range (.totalHits hits)))]
         (document->map (.doc ^IndexSearcher searcher (.doc ^ScoreDoc hit))))))))
