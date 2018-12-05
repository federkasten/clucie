(ns clucie.query
  (:require [clucie.analysis :as analysis]
            [clucie.queryparser :as qp]
            [clucie.utils :refer [stringify-value]])
  (:import [org.apache.lucene.index Term]
           [org.apache.lucene.search
            BooleanClause BooleanClause$Occur BooleanQuery BooleanQuery$Builder
            BoostQuery ConstantScoreQuery DisjunctionMaxQuery Query WildcardQuery]
           [org.apache.lucene.util QueryBuilder]))

(defprotocol FormParsable
  (parse-formt [form options]))

(extend-protocol FormParsable
  Query
  (parse-formt [query _] query)

  clojure.lang.Sequential
  (parse-formt [xs options]
    (let [qb (BooleanQuery$Builder.)]
      (doseq [q (keep #(parse-formt % options) xs)]
        (.add qb q BooleanClause$Occur/MUST))
      (.build qb)))

  clojure.lang.IPersistentSet
  (parse-formt [coll options]
    (let [qb (BooleanQuery$Builder.)]
      (doseq [q (keep #(parse-formt % options) coll)]
        (.add qb q BooleanClause$Occur/SHOULD))
      (.build qb)))

  clojure.lang.IPersistentMap
  (parse-formt [coll options]
    (let [qb (BooleanQuery$Builder.)]
      (doseq [q (keep (fn [[k v]] (parse-formt v (assoc options :key k))) coll)]
        (.add qb q BooleanClause$Occur/MUST))
      (.build qb)))

  String
  (parse-formt [s {:keys [^QueryBuilder builder mode key]}]
    (let [k (stringify-value key)]
      (case mode
        :query (.createBooleanQuery builder k s)
        :phrase-query (.createPhraseQuery builder k s)
        :wildcard-query (WildcardQuery. (Term. k s))
        :qp-query (qp/parse-query (.getAnalyzer builder) k s)
        (throw (ex-info (str "Invalid mode " mode) {:mode mode}))))))

(defn ^Query parse-form
  "Parses form, returning an org.apache.lucene.search.Query.

  The form is parsed according to the following rules:

    org.apache.lucene.search.Query => as is
    [query1 query2] => query1 AND query2
    #{query1 query2} => query1 OR query2
    {:field \"value\"} => query(field:value)

  Options:

    {:analyzer  An org.apache.lucene.analysis.Analyzer instance used for building
                query from string, default (clucie.analysis/standard-analyzer).
     :mode      Changes the behavior of parsing string, default :query.
                <:query|:phrase-query|:wildcard-query|:qp-query>}

  You can extend the behavior of parse-form by implementing parse-formt of
  FormParsable protocol. For example,

    (extend-protocol clucie.query/FormParsable
      java.util.regex.Pattern
      (parse-formt [re {:keys [key]}]
        (org.apache.lucene.search.RegexpQuery.
         (org.apache.lucene.index.Term. (stringify-value key) (.pattern re)))))

  adds a new rule that builds an org.apache.lucene.search.RegexpQuery from a
  regexp."
  [form & {:keys [analyzer mode] :or {analyzer (analysis/standard-analyzer)
                                      mode :query}}]
  (parse-formt form {:builder (QueryBuilder. analyzer)
                     :mode mode
                     :key nil}))

(defn ^Query walk
  "Traverses query. inner and outer are functions. Applies inner to each
  sub-query of query, building up a query of the same type, then applies outer
  to the result."
  [inner outer query]
  (outer
   (condp instance? query
     BooleanQuery
     (let [^BooleanQuery query query
           qb (BooleanQuery$Builder.)]
       (doseq [^BooleanClause clause (.clauses query)]
         (when-let [q (inner (.getQuery clause))]
           (.add qb q (.getOccur clause))))
       (.build qb))

     BoostQuery
     (let [^BoostQuery query query]
       (when-let [q (inner (.getQuery query))]
         (BoostQuery. q (.getBoost query))))

     ConstantScoreQuery
     (let [^ConstantScoreQuery query query]
       (when-let [q (inner (.getQuery query))]
         (ConstantScoreQuery. q)))

     DisjunctionMaxQuery
     (let [^DisjunctionMaxQuery query query]
       (DisjunctionMaxQuery. (keep inner (.getDisjuncts query))
                             (.getTieBreakerMultiplier query)))

     query)))

(defn ^Query postwalk
  "Performs a depth-first, post-order traversal of query. Calls f on each
  sub-query, uses f's return value in place of the original."
  [f query]
  (walk (partial postwalk f) f query))

(defn ^Query prewalk
  "Like postwalk, but does pre-order traversal."
  [f query]
  (walk (partial prewalk f) identity (f query)))

(defn ^Query postwalk-demo
  "Demonstrates the behavior of postwalk by printing each query as it is walked.
  Returns query."
  [query]
  (postwalk (fn [x] (print "Walked: ") (prn x) x) query))

(defn ^Query prewalk-demo
  "Demonstrates the behavior of prewalk by printing each query as it is walked.
  Returns query."
  [query]
  (prewalk (fn [x] (print "Walked: ") (prn x) x) query))
