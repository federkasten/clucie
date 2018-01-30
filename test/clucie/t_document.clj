(ns clucie.t-document
  (:require [midje.sweet :refer :all]
            [clucie.document :as doc])
  (:import [org.apache.lucene.document Document Field$Store StringField]))

(facts "document"
  (tabular
   (fact "returns org.apache.lucene.document.Document"
     (doc/document ?m ?ks) => #(instance? Document %))
   ?m ?ks
   {:key "123", :doc "abc"} [:key :doc]
   {:key 123, :doc "abc"} [:key :doc]
   {:key :123, :doc "abc"} [:key :doc]
   {:key "123", :clucie.core/raw-fields [(StringField. "doc" "abc" Field$Store/YES)]} [:key])
  (fact "throws exception"
    (doc/document {:key "123", :clucie.core/raw-fields [{:doc "abc"}]} [:key]) => (throws Exception)))
