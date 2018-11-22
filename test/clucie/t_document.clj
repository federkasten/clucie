(ns clucie.t-document
  (:require [midje.sweet :refer :all]
            [clucie.document :as doc])
  (:import [org.apache.lucene.document Document Field Field$Store StringField]
           [org.apache.lucene.index IndexOptions]))

(facts "field-type"
  (tabular
   (fact ":indexed?"
     (.indexOptions (doc/field-type ?map)) => ?expected)
   ?map                                    ?expected
   {:indexed? true}                        IndexOptions/DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS
   {:indexed? false}                       IndexOptions/NONE
   {:indexed? IndexOptions/DOCS_AND_FREQS} IndexOptions/DOCS_AND_FREQS
   {}                                      IndexOptions/NONE)
  (tabular
   (fact ":stored?"
     (.stored (doc/field-type ?map)) => ?expected)
   ?map             ?expected
   {:stored? true}  truthy
   {:stored? false} falsey
   {}               truthy)
  (tabular
   (fact ":tokenized?"
     (.tokenized (doc/field-type ?map)) => ?expected)
   ?map                ?expected
   {:tokenized? true}  truthy
   {:tokenized? false} falsey
   {}                  falsey))

(facts "field"
  (tabular
   (fact "returns org.apache.lucene.document.Field"
     (doc/field ?k ?v) => #(instance? Field %))
   ?k ?v
   :key "123"
   "key" 123
   'key 'v
   'key 'x/v
   'x/key 'v
   'x/key 'x/v
   :key :v
   :key ::v
   ::key :v
   ::key ::v
   (fact "throws exception"
     (doc/field nil "123") => (throws Exception))))

(facts "document"
  (tabular
   (fact "returns org.apache.lucene.document.Document"
     (doc/document ?m ?ks) => #(instance? Document %))
   ?m ?ks
   {:key "123", :doc "abc"} [:key :doc]
   {:key 123, :doc "abc"} [:key :doc]
   {'key 'v, :doc "abc"} ['key :doc]
   {'key 'x/v, :doc "abc"} ['key :doc]
   {'x/key 'x, :doc "abc"} ['x/key :doc]
   {'x/key 'x/v, :doc "abc"} ['x/key :doc]
   {:key :123, :doc "abc"} [:key :doc]
   {:key ::123, :doc "abc"} [:key :doc]
   {::key :123, :doc "abc"} [::key :doc]
   {::key ::123, :doc "abc"} [::key :doc]
   {:key "123", :clucie.core/raw-fields [(StringField. "doc" "abc" Field$Store/YES)]} [:key])
  (fact "throws exception"
    (doc/document {:key "123", :clucie.core/raw-fields [{:doc "abc"}]} [:key]) => (throws Exception)))

(fact "document->map"
  (doc/document->map (doc/document {:key "123", :doc "abc"} [:key :doc])) => map?)
