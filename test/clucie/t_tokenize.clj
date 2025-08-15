(ns clucie.t-tokenize
  (:require [clojure.test :refer :all]
            [clucie.analysis :as analysis]
            [clucie.t-common :as t-common]
            [clucie.t-fixture :as t-fixture])
  (:import [org.apache.lucene.analysis TokenStream]
           [org.apache.lucene.analysis.ja JapaneseTokenizer$Mode]))

(deftest test-kuromoji-tokenizer
  (testing "without optional args"
    (let [text t-fixture/tokenize-wo-optargs-text
          result t-fixture/tokenize-wo-optargs-result]
      (is (= result (analysis/kuromoji-tokenize text)))))
  (testing "with optional args"
    (let [text t-fixture/tokenize-w-optargs-text
          user-dict nil
          factory TokenStream/DEFAULT_TOKEN_ATTRIBUTE_FACTORY
          r1 t-fixture/tokenize-w-optargs-false-normal
          r2 t-fixture/tokenize-w-optargs-false-extended
          r3 t-fixture/tokenize-w-optargs-false-search
          r4 t-fixture/tokenize-w-optargs-true-normal
          r5 t-fixture/tokenize-w-optargs-true-extended
          r6 t-fixture/tokenize-w-optargs-true-search]
      (is (= r1 (analysis/kuromoji-tokenize text user-dict false JapaneseTokenizer$Mode/NORMAL factory)))
      (is (= r1 (analysis/kuromoji-tokenize text user-dict false :normal factory)))
      (is (= r2 (analysis/kuromoji-tokenize text user-dict false :extended factory)))
      (is (= r3 (analysis/kuromoji-tokenize text user-dict false :search factory)))
      (is (= r4 (analysis/kuromoji-tokenize text user-dict true :normal factory)))
      (is (= r5 (analysis/kuromoji-tokenize text user-dict true :extended factory)))
      (is (= r6 (analysis/kuromoji-tokenize text user-dict true :search factory))))))
