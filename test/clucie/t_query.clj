(ns clucie.t-query
  (:require [midje.sweet :refer :all]
            [clucie.query :as query])
  (:import [org.apache.lucene.index Term]
           [org.apache.lucene.search
            BooleanClause$Occur BooleanQuery$Builder BoostQuery
            ConstantScoreQuery DisjunctionMaxQuery Query TermQuery]))

;; a:1 (+b:2 +c:3) (d:4)^2.0 ConstantScore(e:5) (f:6 | g:7)~0.1
(def test-query
  (let [qb (BooleanQuery$Builder.)]
    (doto qb
      (.add (TermQuery. (Term. "a" "1")) BooleanClause$Occur/SHOULD)
      (.add (let [qb2 (BooleanQuery$Builder.)]
              (doto qb2
                (.add (TermQuery. (Term. "b" "2")) BooleanClause$Occur/MUST)
                (.add (TermQuery. (Term. "c" "3")) BooleanClause$Occur/MUST))
              (.build qb2))
            BooleanClause$Occur/SHOULD)
      (.add (BoostQuery. (TermQuery. (Term. "d" "4")) 2.0) BooleanClause$Occur/SHOULD)
      (.add (ConstantScoreQuery. (TermQuery. (Term. "e" "5"))) BooleanClause$Occur/SHOULD)
      (.add (DisjunctionMaxQuery. [(TermQuery. (Term. "f" "6"))
                                   (TermQuery. (Term. "g" "7"))]
                                  0.1)
            BooleanClause$Occur/SHOULD))
    (.build qb)))

(fact "walk"
  (query/walk identity identity test-query) => test-query
  (query/walk identity identity test-query) => #(instance? Query %)
  (query/walk identity identity nil) => nil?)

(facts "postwalk"
  (query/postwalk identity test-query) => #(instance? Query %)
  (query/postwalk identity nil) => nil?
  (fact "traversal order"
    (let [xs (atom [])]
      (query/postwalk (fn [x] (swap! xs conj (str x)) x) test-query)
      @xs)
    => ["a:1"
        "b:2"
        "c:3"
        "+b:2 +c:3"
        "d:4"
        "(d:4)^2.0"
        "e:5"
        "ConstantScore(e:5)"
        "f:6"
        "g:7"
        "(f:6 | g:7)~0.1"
        "a:1 (+b:2 +c:3) (d:4)^2.0 ConstantScore(e:5) (f:6 | g:7)~0.1"])
  (fact "transform"
    (str
     (query/postwalk
      (fn [x]
        (condp instance? x
          TermQuery (let [^Term t (.getTerm ^TermQuery x)]
                      (TermQuery. (Term. (.field t) (-> (Integer/parseInt (.text t)) inc str))))
          BoostQuery nil
          ConstantScoreQuery (let [^TermQuery q (.getQuery ^ConstantScoreQuery x)
                                   ^Term t (.getTerm q)]
                               (ConstantScoreQuery. (TermQuery. (Term. "z" (.text t)))))
          x))
      test-query))
    => "a:2 (+b:3 +c:4) ConstantScore(z:6) (f:7 | g:8)~0.1"))

(facts "prewalk"
  (query/prewalk identity test-query) => #(instance? Query %)
  (query/prewalk identity nil) => nil?
  (fact "traversal order"
    (let [xs (atom [])]
      (query/prewalk (fn [x] (swap! xs conj (str x)) x) test-query)
      @xs)
    => ["a:1 (+b:2 +c:3) (d:4)^2.0 ConstantScore(e:5) (f:6 | g:7)~0.1"
        "a:1"
        "+b:2 +c:3"
        "b:2"
        "c:3"
        "(d:4)^2.0"
        "d:4"
        "ConstantScore(e:5)"
        "e:5"
        "(f:6 | g:7)~0.1"
        "f:6"
        "g:7"])
  (fact "transform"
    (str
     (query/prewalk
      (fn [x]
        (condp instance? x
          TermQuery (let [^Term t (.getTerm ^TermQuery x)]
                      (TermQuery. (Term. (.field t) (-> (Integer/parseInt (.text t)) inc str))))
          BoostQuery nil
          ConstantScoreQuery (let [^TermQuery q (.getQuery ^ConstantScoreQuery x)
                                   ^Term t (.getTerm q)]
                               (ConstantScoreQuery. (TermQuery. (Term. "z" (.text t)))))
          x))
      test-query))
    => "a:2 (+b:3 +c:4) ConstantScore(z:6) (f:7 | g:8)~0.1"))
