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
           [java.io File]
           [org.apache.lucene.store NIOFSDirectory Directory]
           [org.apache.lucene.analysis.util CharArraySet]
           [org.apache.lucene.analysis TokenStream]
           [org.apache.lucene.analysis.ja JapaneseTokenizer$Mode]))

(def test-store (atom nil))

(def all-entries
  [["1" "20130819"] ; NB: this entry is like #3 (match both)
   ["2" "佐藤先生"]
   ["3" "実験済み(20140723)"] ; NB: this entry is like #1
   ["4" "これは形態素解析の試験用エントリです。alphabetic wordsもあります。"]
   ["5" "ぬふあうえおやゆよわほへー\n\tたていすかんなにらせ"]])

(def entry1 (nth all-entries 0))
(def entry2 (nth all-entries 1))
(def entry3 (nth all-entries 2))
(def entry4 (nth all-entries 3))
(def entry5 (nth all-entries 4))

(defn- tidy-ascii-name [n]
  (cstr/join " "
             (map #(cstr/replace % #"(_|-|\.)" " ")
                  (re-seq #"\w+" n))))

(def ^:dynamic entry-analyzer
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

(defn- search-entries [query-string max-num & [page results-per-page]]
  (core/search @test-store
               [{:doc query-string}
                {:ascii-name (tidy-ascii-name query-string)}]
               max-num
               entry-analyzer
               page
               results-per-page))

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

(defn- close-test-store! []
  (.close ^Directory @test-store))

(defn- finish-store! [& [path]]
  (when @test-store
    (close-test-store!)
    (when path
      (delete-dir! (io/file path)))
    (reset! test-store nil)))

(defn- prepare-store! [& [path dont-add-test-entries?]]
  (when @test-store
    (finish-store! path))
  (let [store (if path
                (store/disk-store path)
                (store/memory-store))]
    (reset! test-store store)
    (when-not dont-add-test-entries?
      (add-all-test-entries!))))

(defn- results-is-valid? [quantity & [entry-key]]
  (if (or (zero? quantity) (not entry-key))
    #(= quantity (count %))
    (fn [results]
      (and
        (= quantity (count results))
        (boolean (first (filter #(= entry-key (:key %))
                                results)))))))

(with-state-changes [(before :facts (prepare-store!))
                     (after :facts (finish-store!))]
  (facts "add new entries and search entries"
    (fact "search exists entries"
      (search-entries "2013" 10) => (results-is-valid? 2 (first entry1))
      (search-entries "佐藤先生" 10) => (results-is-valid? 1 (first entry2))
      (search-entries "佐藤" 10) => (results-is-valid? 1 (first entry2))
      (search-entries "藤先" 10) => (results-is-valid? 1 (first entry2))
      (search-entries "先生" 10) => (results-is-valid? 1 (first entry2)))
    (fact "search new entries"
      (let [entry-key "9999"
            entry-doc "テスト"]
        (search-entries entry-doc 10) => (results-is-valid? 0)
        (add-entry! entry-key entry-doc) => nil
        (search-entries entry-doc 10) => (results-is-valid? 1 entry-key)))
    (fact "search new entries with non-string keys"
      (let [entry-key 34567
            entry-doc "34567"]
        (search-entries entry-doc 10) => (results-is-valid? 0)
        (add-entry! entry-key entry-doc) => nil
        (search-entries entry-doc 10) => (results-is-valid? 1 (str entry-key)))
      (let [entry-key :test
            entry-doc ":test"]
        (search-entries entry-doc 10) => (results-is-valid? 0)
        (add-entry! entry-key entry-doc) => nil
        (search-entries entry-doc 10) => (results-is-valid? 1 (name entry-key)))
      (let [entry-key (java.util.UUID/randomUUID)
            entry-doc "random"]
        (search-entries entry-doc 10) => (results-is-valid? 0)
        (add-entry! entry-key entry-doc) => nil
        (search-entries entry-doc 10) => (results-is-valid? 1 (str entry-key))))
    (fact "search with pagination"
      (let [doc-prefix "ページング用"]
        (dotimes [i 105]
          (let [index (+ i 50000)
                doc (str doc-prefix index)]
            (add-entry! (str index) doc)))
        (search-entries doc-prefix 100 0 10) => (results-is-valid? 10)
        (search-entries doc-prefix 19 1 10) => (results-is-valid? 9)
        (search-entries doc-prefix 200 10 10) => (results-is-valid? 5)
        (search-entries doc-prefix 200 0 200) => (results-is-valid? 105)
        (search-entries doc-prefix 200 200 200) => (results-is-valid? 0)
        (search-entries doc-prefix 5 200 5) => (results-is-valid? 0)
        (search-entries doc-prefix 8 0 200) => (results-is-valid? 8)))))

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

(binding [entry-analyzer nil]
  (with-state-changes [(before :facts (prepare-store! nil true))
                       (after :facts (finish-store!))]
    (facts "manipulate with default analyzer"
      (let [new-key "100"
            new-document1 "apple"
            new-document2 "orange"]
        (core/add! @test-store
                   [{:key new-key
                     :doc new-document1
                     :ascii-name (tidy-ascii-name new-document1)}]
                   [:key :doc :ascii-name])
        (search-entries new-document1 10) => (results-is-valid? 1 new-key)
        (search-entries new-document2 10) => (results-is-valid? 0)
        (core/update! @test-store
                      {:key new-key
                       :doc new-document2
                       :ascii-name (tidy-ascii-name new-document2)}
                      [:key :doc :ascii-name]
                      :key new-key)
        (search-entries new-document1 10) => (results-is-valid? 0)
        (search-entries new-document2 10) => (results-is-valid? 1 new-key)
        (core/delete! @test-store :key new-key)
        (search-entries new-document1 10) => (results-is-valid? 0)
        (search-entries new-document2 10) => (results-is-valid? 0)))))

(with-state-changes [(before :facts (prepare-store!))
                     (after :facts (finish-store!))]
  (facts "add/update without index"
    (let [new-key "100"
          new-document1 "林檎"
          new-document2 "蜜柑"]
      (search-entries new-document1 10) => (results-is-valid? 0)
      (search-entries new-document2 10) => (results-is-valid? 0)
      (core/add! @test-store
                 [{:key new-key
                   :doc new-document1
                   :ascii-name (tidy-ascii-name new-document1)}]
                 [])
      (search-entries new-document1 10) => (results-is-valid? 0)
      (search-entries new-document2 10) => (results-is-valid? 0)
      (core/update! @test-store
                    {:key new-key
                     :doc new-document2
                     :ascii-name (tidy-ascii-name new-document2)}
                    []
                    :key new-key)
      (search-entries new-document1 10) => (results-is-valid? 0)
      (search-entries new-document2 10) => (results-is-valid? 0)
      (core/delete! @test-store :key new-key)
      (search-entries new-document1 10) => (results-is-valid? 0)
      (search-entries new-document2 10) => (results-is-valid? 0))))

(let [tmp-store-path (get-tmp-dir)]
  (with-state-changes [(before :facts (prepare-store! tmp-store-path))
                       (after :facts (finish-store! tmp-store-path))]
    (facts "disk store"
      (let [[old-key old-doc] entry2
            new-key "9998"
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
        ;; Switch to another session
        (do
          (close-test-store!)
          (reset! test-store (store/disk-store tmp-store-path))
          nil) => nil
        (search-entries new-doc 10) => (results-is-valid? 1 new-key)))))

(facts "kuromoji tokenizer"
  ;; (doseq [doc (map second all-entries)]
  ;;   (prn (analysis/kuromoji-tokenize doc)))
  (fact "kuromoji tokenize"
    (let [text "牛焼肉定食"
          result ["牛" "焼肉" "定食"]]
      (analysis/kuromoji-tokenize text) => result))
  (fact "kuromoji tokenize with factory"
    (let [text "ギュウヤキニク、牛焼肉定食です。"
          result ["ギ" "ュ" "ウ" "ヤ" "キ" "ニ" "ク" "牛" "焼肉" "定食" "です"]
          factory TokenStream/DEFAULT_TOKEN_ATTRIBUTE_FACTORY]
      (analysis/kuromoji-tokenize text nil true :extended factory) => result)))

(binding [entry-analyzer (analysis/analyzer-mapping
                           (analysis/keyword-analyzer)
                           {:doc (analysis/standard-analyzer)
                            :ascii-name (analysis/ngram-analyzer 2 8 [])})]
  (with-state-changes [(before :facts (prepare-store!))
                       (after :facts (finish-store!))]
    (facts "standard analyzer"
      (search-entries "2013" 10) => (results-is-valid? 2 (first entry1))
      (search-entries "佐藤先生" 10) => (results-is-valid? 1 (first entry2))
      (search-entries "佐藤" 10) => (results-is-valid? 1 (first entry2))
      (search-entries "藤先" 10) => (results-is-valid? 1 (first entry2))
      (search-entries "先生" 10) => (results-is-valid? 1 (first entry2)))))

(binding [entry-analyzer (analysis/analyzer-mapping
                           (analysis/keyword-analyzer)
                           {:doc (analysis/standard-analyzer (map identity "先生"))
                            :ascii-name (analysis/ngram-analyzer 2 8 [])})]
  (with-state-changes [(before :facts (prepare-store!))
                       (after :facts (finish-store!))]
    (facts "standard analyzer with stop-words"
      (search-entries "2013" 10) => (results-is-valid? 2 (first entry1))
      (search-entries "佐藤先生" 10) => (results-is-valid? 1 (first entry2))
      (search-entries "佐藤" 10) => (results-is-valid? 1 (first entry2))
      (search-entries "藤先" 10) => (results-is-valid? 1 (first entry2))
      (search-entries "先生" 10) => (results-is-valid? 0))))

(binding [entry-analyzer (analysis/analyzer-mapping
                           (analysis/keyword-analyzer)
                           {:doc (analysis/cjk-analyzer (map identity "先生"))
                            :ascii-name (analysis/ngram-analyzer 2 8 [])})]
  (with-state-changes [(before :facts (prepare-store!))
                       (after :facts (finish-store!))]
    (facts "cjk analyzer with stop-words"
      (search-entries "2013" 10) => (results-is-valid? 2 (first entry1))
      (search-entries "佐藤先生" 10) => (results-is-valid? 1 (first entry2))
      (search-entries "佐藤" 10) => (results-is-valid? 1 (first entry2))
      (search-entries "藤先" 10) => (results-is-valid? 1 (first entry2))
      (search-entries "先生" 10) => (results-is-valid? 1 (first entry2)))))

(binding [entry-analyzer (analysis/analyzer-mapping
                           (analysis/keyword-analyzer)
                           {:doc (analysis/kuromoji-analyzer)
                            :ascii-name (analysis/ngram-analyzer 2 8 [])})]
  (with-state-changes [(before :facts (prepare-store!))
                       (after :facts (finish-store!))]
    (facts "kuromoji analyzer"
      (search-entries "2013" 10) => (results-is-valid? 2 (first entry1))
      (search-entries "佐藤先生" 10) => (results-is-valid? 1 (first entry2))
      (search-entries "佐藤" 10) => (results-is-valid? 1 (first entry2))
      (search-entries "藤先" 10) => (results-is-valid? 0)
      (search-entries "先生" 10) => (results-is-valid? 1 (first entry2)))))

(binding [entry-analyzer (analysis/analyzer-mapping
                           (analysis/keyword-analyzer)
                           {:doc (analysis/kuromoji-analyzer
                                   nil
                                   JapaneseTokenizer$Mode/SEARCH
                                   (CharArraySet. (map identity "先生") false)
                                   #{})
                            :ascii-name (analysis/ngram-analyzer 2 8 [])})]
  (with-state-changes [(before :facts (prepare-store!))
                       (after :facts (finish-store!))]
    (facts "kuromoji analyzer with stop-words"
      (search-entries "2013" 10) => (results-is-valid? 2 (first entry1))
      (search-entries "佐藤先生" 10) => (results-is-valid? 1 (first entry2))
      (search-entries "佐藤" 10) => (results-is-valid? 1 (first entry2))
      (search-entries "藤先" 10) => (results-is-valid? 0)
      (search-entries "先生" 10) => (results-is-valid? 1 (first entry2)))))

