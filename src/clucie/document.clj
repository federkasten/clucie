(ns clucie.document
  (:require [clucie.utils :refer [stringify-value keyword->str qualified?]])
  (:import [org.apache.lucene.document Document Field FieldType]
           [org.apache.lucene.index IndexOptions]))

(defn- estimate-value
  [v]
  (cond
    (string? v) {:value v :value-type :string}
    (integer? v) {:value (str v) :value-type :integer}
    (keyword? v) {:value (keyword->str v) :value-type :keyword}
    :else {:value (str v) :value-type :unknown}))

(defn ^FieldType field-type
  "Creates an org.apache.lucene.document.FieldType according to given map:

    {:indexed?    Set true if the field should be indexed, default false. This
                  accepts an org.apache.lucene.index.IndexOptions constant other
                  than boolean value.
     :stored?     Set true if the field should be stored, default true.
     :tokenized?  Set true if the field should be tokenized, default false.}"
  [{:keys [indexed? stored? tokenized?]
    :or {indexed? false, stored? true, tokenized? false}}]
  (let [index-opt (cond
                    (true? indexed?) IndexOptions/DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS
                    (false? indexed?) IndexOptions/NONE
                    :else indexed?)]
    (doto (FieldType.)
      (.setIndexOptions index-opt)
      (.setStored stored?)
      (.setTokenized tokenized?))))

(defn ^Field field
  "Creates an org.apache.lucene.document.Field from key and value. opts is an
  optional map specifying type of the field, which is passed to field-type
  function as it is. See also docs of field-type for further details."
  ([key value] (field key value {}))
  ([key value opts]
   (let [{:keys [^String value]} (estimate-value value)]
     (Field. (stringify-value key) value (field-type opts)))))

(defn ^Document document
  "Creates an org.apache.lucene.document.Document from a map. The map can
  optionally includes :clucie.core/raw-fields key, which value must be a
  sequence of raw org.apache.lucene.document.Field instances."
  [m keys]
  (let [doc (Document.)
        fs (concat (map (fn [[k v]]
                          (let [exists? (contains? keys k)]
                            (field k v {:indexed? exists?
                                        :stored? true
                                        :tokenized? (and exists? (not (qualified? v)))})))
                        (dissoc m :clucie.core/raw-fields))
                   (:clucie.core/raw-fields m))]
    (doseq [^Field f fs]
      (.add doc f))
    doc))

(defn document->map
  "Turns a Document object into a map."
  [^Document doc]
  (into {} (for [^Field f (.getFields doc)]
             [(keyword (.name f)) (.stringValue f)])))
