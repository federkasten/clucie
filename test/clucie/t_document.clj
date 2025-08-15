(ns clucie.t-document
  (:require [clojure.test :refer :all]
            [clucie.document :as doc])
  (:import [org.apache.lucene.document Document Field Field$Store StringField]
           [org.apache.lucene.index IndexOptions]))

(deftest test-field-type
  (testing ":indexed?"
    (let [test-cases [
          [{:indexed? true}                        IndexOptions/DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS]
          [{:indexed? false}                       IndexOptions/NONE]
          [{:indexed? IndexOptions/DOCS_AND_FREQS} IndexOptions/DOCS_AND_FREQS]
          [{}                                      IndexOptions/NONE]]]
      (doseq [[map expected] test-cases]
        (is (= expected (.indexOptions (doc/field-type map)))))))
  
  (testing ":stored?"
    (let [test-cases [
          [{:stored? true}  true]
          [{:stored? false} false]
          [{}               true]]]
      (doseq [[map expected] test-cases]
        (is (= expected (.stored (doc/field-type map)))))))
  
  (testing ":tokenized?"
    (let [test-cases [
          [{:tokenized? true}  true]
          [{:tokenized? false} false]
          [{}                  false]]]
      (doseq [[map expected] test-cases]
        (is (= expected (.tokenized (doc/field-type map))))))))

(deftest test-field
  (testing "returns org.apache.lucene.document.Field"
    (let [test-cases [
          [:key "123"]
          ["key" 123]
          ['key 'v]
          ['key 'x/v]
          ['x/key 'v]
          ['x/key 'x/v]
          [:key :v]
          [:key ::v]
          [::key :v]
          [::key ::v]]]
      (doseq [[k v] test-cases]
        (is (instance? Field (doc/field k v))))))
  
  (testing "throws exception"
    (is (thrown? Exception (doc/field nil "123")))))

(deftest test-document
  (testing "returns org.apache.lucene.document.Document"
    (let [test-cases [
          [{:key "123", :doc "abc"} [:key :doc]]
          [{:key 123, :doc "abc"} [:key :doc]]
          [{'key 'v, :doc "abc"} ['key :doc]]
          [{'key 'x/v, :doc "abc"} ['key :doc]]
          [{'x/key 'x, :doc "abc"} ['x/key :doc]]
          [{'x/key 'x/v, :doc "abc"} ['x/key :doc]]
          [{:key :123, :doc "abc"} [:key :doc]]
          [{:key ::123, :doc "abc"} [:key :doc]]
          [{::key :123, :doc "abc"} [::key :doc]]
          [{::key ::123, :doc "abc"} [::key :doc]]
          [{:key "123", :clucie.core/raw-fields [(StringField. "doc" "abc" Field$Store/YES)]} [:key]]]]
      (doseq [[m ks] test-cases]
        (is (instance? Document (doc/document m ks))))))
  
  (testing "throws exception"
    (is (thrown? Exception (doc/document {:key "123", :clucie.core/raw-fields [{:doc "abc"}]} [:key])))))

(deftest test-document->map
  (testing "document->map"
    (is (map? (doc/document->map (doc/document {:key "123", :doc "abc"} [:key :doc]))))))
