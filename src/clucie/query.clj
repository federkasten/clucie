(ns clucie.query
  (:import [org.apache.lucene.search
            BooleanClause BooleanQuery BooleanQuery$Builder BoostQuery
            ConstantScoreQuery DisjunctionMaxQuery Query]))

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
