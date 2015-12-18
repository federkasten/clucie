(ns clucie.t-core
  (:require [midje.sweet :refer :all]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as cstr]
            [clojure.pprint :as pprint]
            [clucie.core :as core]
            [clucie.analysis :as analysis]
            [clucie.store :as store])
  (:import [java.util UUID]
           [java.io File]))

(def test-store (atom nil))

;;; TODO: Add more data and tests
(def all-entries
  [["1" "20130819"] ; NB: this entry is like #3 (match both)
   ["2" "佐藤先生"]
   ["3" "実験済み(20140723)"] ; NB: this entry is like #1
   ])

(def entry1 (nth all-entries 0))
(def entry2 (nth all-entries 1))
(def entry3 (nth all-entries 2))

(defn- tidy-ascii-name [n]
  (cstr/join " "
             (map #(cstr/replace % #"(_|-|\.)" " ")
                  (re-seq #"\w+" n))))

(def entry-analyzer
  (analysis/analyzer-mapping (analysis/keyword-analyzer)
                             {:doc (analysis/cjk-analyzer)
                              :ascii-name (analysis/ngram-analyzer 2 8 [])}))

(defn- add-entry! [k document]
  (core/add! @test-store
             [{:key k
               :doc document
               :ascii-name (tidy-ascii-name document)}]
             [:key :doc :ascii-name]
             entry-analyzer))

(defn- add-all-test-entries! []
  (doseq [entry all-entries]
    (apply add-entry! entry)))

(defn- update-entry! [k document]
  (core/update! @test-store
                {:key k
                 :doc document
                 :ascii-name (tidy-ascii-name document)}
                [:key :doc :ascii-name]
                :key k
                entry-analyzer))

(defn- delete-entry! [k]
  (core/delete! @test-store
                :key k
                entry-analyzer))

(defn- search-entries [query-string max-num]
  (core/search @test-store
               [{:doc query-string}
                {:ascii-name (tidy-ascii-name query-string)}]
               max-num
               entry-analyzer))

(defn get-tmp-dir [& [specific-dir]]
  (loop []
    (let [uuid4 (.toString (UUID/randomUUID))
          tmpdir (System/getProperty "java.io.tmpdir")
          d (if specific-dir
              (str tmpdir File/separator specific-dir File/separator uuid4)
              (str tmpdir File/separator uuid4))]
      (if (.exists (io/file d))
        (do
          (Thread/sleep 100) ;; Wait to put a clock forward
          (recur))
        d))))

(defn delete-dir! [^File dir-or-file]
  (when (.isDirectory dir-or-file)
    (doseq [child (.listFiles dir-or-file)]
      (delete-dir! child)))
  (.delete dir-or-file))

(defn- finish-store! [& [path]]
  (when @test-store
    (.close @test-store))
    (when path
      (delete-dir! (io/file path)))
    (reset! test-store nil))

(defn- prepare-store! [& [path]]
  (when @test-store
    (finish-store! path))
  (let [store (if path
                (store/disk-store path)
                (store/memory-store))]
    (reset! test-store store)
    (add-all-test-entries!)))

(defn- results-is-valid? [quantity & [entry-key]]
  (if (or (zero? quantity) (not entry-key))
    #(= quantity (count %))
    (fn [results]
      (and
        (= quantity (count results))
        (boolean (first (filter #(= entry-key (:key %))
                                results)))))))

;;; TODO: Add test to check :ascii-name
;;; TODO: Add more tests

(with-state-changes [(before :facts (prepare-store!))
                     (after :facts (finish-store!))]
  (facts "add new entries and search entries"
    (fact "search exists entries"
      (search-entries "2013" 10) => (results-is-valid? 2 (first entry1))
      (search-entries "佐藤" 10) => (results-is-valid? 1 (first entry2)))
    (fact "search new entries"
      (let [entry-key "4"
            entry-doc "テスト"]
        (search-entries entry-doc 10) => (results-is-valid? 0)
        (add-entry! entry-key entry-doc) => nil
        (search-entries entry-doc 10) => (results-is-valid? 1 entry-key)))))

(with-state-changes [(before :facts (prepare-store!))
                     (after :facts (finish-store!))]
  (facts "update entry document"
    (let [entry-key (first entry1)
          new-entry-doc "新しい日本語テキスト"]
      (search-entries "20130819" 10) => (results-is-valid? 2 entry-key)
      (search-entries new-entry-doc 10) => (results-is-valid? 0)
      (update-entry! entry-key new-entry-doc) => nil
      (search-entries "20130819" 10) => (results-is-valid? 1)
      (search-entries new-entry-doc 10) => (results-is-valid? 1 entry-key))))

(with-state-changes [(before :facts (prepare-store!))
                     (after :facts (finish-store!))]
  (facts "delete entry"
    (fact "entry1"
      (search-entries (second entry1) 10) => (results-is-valid? 2)
      (delete-entry! (first entry1)) => nil
      (search-entries (second entry1) 10) => (results-is-valid? 1))
    (fact "entry2"
      (search-entries (second entry2) 10) => (results-is-valid? 1)
      (delete-entry! (first entry2)) => nil
      (search-entries (second entry2) 10) => (results-is-valid? 0))
    (fact "entry3"
      (search-entries (second entry3) 10) => (results-is-valid? 2)
      (delete-entry! (first entry3)) => nil
      (search-entries (second entry3) 10) => (results-is-valid? 1)
      (delete-entry! (first entry1)) => nil
      (search-entries (second entry3) 10) => (results-is-valid? 0))))

(let [tmp-store-path (get-tmp-dir)]
  (with-state-changes [(before :facts (prepare-store! tmp-store-path))
                       (after :facts (finish-store! tmp-store-path))]
    (facts "disk store"
      (let [[old-key old-doc] entry2
            new-key "5"
            new-doc "同一内容"]
        (search-entries old-doc 10) => (results-is-valid? 1 old-key)
        (search-entries new-doc 10) => (results-is-valid? 0)
        (update-entry! old-key new-doc) => nil
        (search-entries old-doc 10) => (results-is-valid? 0)
        (search-entries new-doc 10) => (results-is-valid? 1 old-key)
        (add-entry! new-key new-doc) => nil
        (search-entries new-doc 10) => (results-is-valid? 2 new-key)
        (delete-entry! old-key) => nil
        (search-entries new-doc 10) => (results-is-valid? 1 new-key)
        ;; Switch another session
        (do
          (.close @test-store)
          (reset! test-store (store/disk-store tmp-store-path))
          nil) => nil
        (search-entries new-doc 10) => (results-is-valid? 1 new-key)))))
