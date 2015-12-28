(ns clucie.t-tokenize
  (:require [midje.sweet :refer :all]
            [clucie.analysis :as analysis]
            [clucie.t-common :as t-common]
            [clucie.t-fixture :as t-fixture])
  (:import [org.apache.lucene.analysis TokenStream]
           [org.apache.lucene.analysis.ja JapaneseTokenizer$Mode]))

(facts "kuromoji tokenizer"
  (fact "without optional args"
    (let [text t-fixture/tokenize-wo-optargs-text
          result t-fixture/tokenize-wo-optargs-result]
      (analysis/kuromoji-tokenize text) => result))
  (fact "with optional args"
    (let [text t-fixture/tokenize-w-optargs-text
          user-dict nil
          factory TokenStream/DEFAULT_TOKEN_ATTRIBUTE_FACTORY
          r1 t-fixture/tokenize-w-optargs-false-normal
          r2 t-fixture/tokenize-w-optargs-false-extended
          r3 t-fixture/tokenize-w-optargs-false-search
          r4 t-fixture/tokenize-w-optargs-true-normal
          r5 t-fixture/tokenize-w-optargs-true-extended
          r6 t-fixture/tokenize-w-optargs-true-search]
      (analysis/kuromoji-tokenize text user-dict false JapaneseTokenizer$Mode/NORMAL factory) => r1
      (analysis/kuromoji-tokenize text user-dict false :normal factory) => r1
      (analysis/kuromoji-tokenize text user-dict false :extended factory) => r2
      (analysis/kuromoji-tokenize text user-dict false :search factory) => r3
      (analysis/kuromoji-tokenize text user-dict true :normal factory) => r4
      (analysis/kuromoji-tokenize text user-dict true :extended factory) => r5
      (analysis/kuromoji-tokenize text user-dict true :search factory) => r6)))
