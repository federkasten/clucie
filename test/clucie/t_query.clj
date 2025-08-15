(ns clucie.t-query
  (:require [clojure.test :refer :all]
            [clucie.query :as query])
  (:import [org.apache.lucene.index Term]
           [org.apache.lucene.search
            BooleanClause$Occur BooleanQuery$Builder BoostQuery
            ConstantScoreQuery DisjunctionMaxQuery Query TermQuery]))

(deftest test-parse-form
  (testing "parses form, returning an org.apache.lucene.search.Query"
    (let [test-cases [
          [{:a "1"}                  :query          "+a:1"]
          [{:a ["1" "2"]}            :query          "+(+a:1 +a:2)"]              ; AND
          [{:a "1" :b "2"}           :query          "+a:1 +b:2"]                 ; AND (multi-keys)
          [[{:a "1"} {:b ["2" "3"]}] :query          "+(+a:1) +(+(+b:2 +b:3))"]   ; AND (combination)
          [{:a #{"1" "2"}}           :query          "+(a:1 a:2)"]                ; OR
          [#{{:a "1"} {:b "2"}}      :query          "(+a:1) (+b:2)"]             ; OR (multi-keys)
          [{:a "1 2"}                :phrase-query   "+a:\"1 2\""]                ; phrase
          [{:a "te?t"}               :wildcard-query "+a:te?t"]                   ; wildcard
          [{:a "1 (+b:2 +c:3^2)"}    :qp-query       "+(a:1 (+b:2 +(c:3)^2.0))"] ; queryparser
          ]]
      (doseq [[form mode expected-str] test-cases]
        (let [q (query/parse-form form :mode mode)]
          (is (instance? Query q))
          (is (= expected-str (str q)))))))
  
  (testing "throws exception"
    (let [error-cases [
          [{:a #"1"} :query]
          [nil       :query]
          [{:a "1"}  :invalid]
          [{:a "1"}  nil]]]
      (doseq [[form mode] error-cases]
        (is (thrown? Exception (query/parse-form form :mode mode)))))))

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

(deftest test-walk
  (testing "walk"
    (is (= test-query (query/walk identity identity test-query)))
    (is (instance? Query (query/walk identity identity test-query)))
    (is (nil? (query/walk identity identity nil)))))

(deftest test-postwalk
  (testing "postwalk"
    (is (instance? Query (query/postwalk identity test-query)))
    (is (nil? (query/postwalk identity nil))))
  
  (testing "traversal order"
    (let [xs (atom [])]
      (query/postwalk (fn [x] (swap! xs conj (str x)) x) test-query)
      (is (= ["a:1"
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
              "a:1 (+b:2 +c:3) (d:4)^2.0 ConstantScore(e:5) (f:6 | g:7)~0.1"]
             @xs))))
  
  (testing "transform"
    (is (= "a:2 (+b:3 +c:4) ConstantScore(z:6) (f:7 | g:8)~0.1"
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
             test-query))))))

(deftest test-prewalk
  (testing "prewalk"
    (is (instance? Query (query/prewalk identity test-query)))
    (is (nil? (query/prewalk identity nil))))
  
  (testing "traversal order"
    (let [xs (atom [])]
      (query/prewalk (fn [x] (swap! xs conj (str x)) x) test-query)
      (is (= ["a:1 (+b:2 +c:3) (d:4)^2.0 ConstantScore(e:5) (f:6 | g:7)~0.1"
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
              "g:7"]
             @xs))))
  
  (testing "transform"
    (is (= "a:2 (+b:3 +c:4) ConstantScore(z:6) (f:7 | g:8)~0.1"
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
             test-query)))))
)
