(ns clucie.t-core
  (:require [clojure.test :refer :all]
            [clucie.core :as core]
            [clucie.analysis :as analysis]
            [clucie.store :as store]
            [clucie.t-common :as t-common]
            [clucie.t-fixture :as t-fixture])
  (:import [java.util UUID]
           [org.apache.lucene.queryparser.flexible.standard StandardQueryParser]))

(defmacro run-testset! [testset-symbol]
  (let [testset (eval testset-symbol)
        label (str (:testname testset) " : " (:testdesc testset))
        tests (map (fn [[query result]]
                     `(testing ~(str ": query = " query)
                        (let [actual# (try
                                        (set (map :key
                                                  (~'search-fn @t-common/test-store
                                                               [{:doc ~query}]
                                                               10
                                                               @t-common/doc-analyzer)))
                                        ;; for check exception
                                        (catch Throwable e# (type e#)))]
                          (is (= ~result actual#)))))
                   (seq (:query+result testset)))]
    `(deftest ~(symbol (str "test-" (:testname testset)))
       (let [testset# ~testset-symbol
             ~'search-fn (:search-fn testset#)]
         (t-common/prepare! (:analyzer testset#)
                            nil
                            (:dataset testset#))
         (testing ~label
           ~@tests)
         (t-common/finish!)))))

(deftest test-keyword-analyzer-with-wildcard-search
  (t-common/prepare! t-common/keyword-analyzer nil t-fixture/entries-en-1)
  (try
    (testing "search exists entries"
      (is ((t-common/results-is-valid? 1 (get-in t-fixture/entries-en-1 [0 0]))
           (t-common/wildcard-search-entries t-fixture/entries-en-1-search-wildcard-1 10)))
      (is ((t-common/results-is-valid? 1 (get-in t-fixture/entries-en-1 [0 0]))
           (t-common/wildcard-search-entries t-fixture/entries-en-1-search-wildcard-2 10)))
      (is ((t-common/results-is-valid? 0)
           (t-common/wildcard-search-entries t-fixture/entries-en-1-search-wildcard-3 10)))
      (is ((t-common/results-is-valid? 1 (get-in t-fixture/entries-en-1 [1 0]))
           (t-common/wildcard-search-entries t-fixture/entries-en-1-search-wildcard-4 10))))
    (testing "search new entries"
      (let [entry-key "9999"
            entry-doc "latest entry"
            search-query "*est ent*"]
        (is ((t-common/results-is-valid? 0)
             (t-common/wildcard-search-entries search-query 10)))
        (is (nil? (t-common/add-entry! entry-key entry-doc)))
        (is ((t-common/results-is-valid? 1 entry-key)
             (t-common/wildcard-search-entries search-query 10)))))
    (finally
      (t-common/finish!))))

(deftest test-standard-analyzer
  (t-common/prepare! t-common/standard-analyzer nil t-fixture/entries-en-1)
  (try
    (testing "search exists entries"
      (is ((t-common/results-is-valid? 1 (get-in t-fixture/entries-en-1 [0 0]))
           (t-common/search-entries t-fixture/entries-en-1-search-1 10)))
      (is ((t-common/results-is-valid? 0)
           (t-common/search-entries t-fixture/entries-en-1-search-2 10)))
      (is ((t-common/results-is-valid? 1 (get-in t-fixture/entries-en-1 [1 0]))
           (t-common/search-entries t-fixture/entries-en-1-search-3 10)))
      (is ((t-common/results-is-valid? 0)
           (t-common/search-entries t-fixture/entries-en-1-search-4 10))))
    (testing "search new entries"
      (let [entry-key "9999"
            entry-doc "latest entry"]
        (is ((t-common/results-is-valid? 0)
             (t-common/search-entries entry-doc 10)))
        (is (nil? (t-common/add-entry! entry-key entry-doc)))
        (is ((t-common/results-is-valid? 1 entry-key)
             (t-common/search-entries entry-doc 10)))))
    (finally
      (t-common/finish!))))

(deftest test-built-analyzer
  (t-common/prepare! t-common/built-analyzer nil t-fixture/entries-unnormalized-1)
  (try
    (testing "search exists entries"
      (is ((t-common/results-is-valid? 1 (get-in t-fixture/entries-unnormalized-1 [0 0]))
           (t-common/search-entries t-fixture/entries-unnormalized-1-search-1 10)))
      (is ((t-common/results-is-valid? 2 (get-in t-fixture/entries-unnormalized-1 [1 0]))
           (t-common/search-entries t-fixture/entries-unnormalized-1-search-2 10)))
      (is ((t-common/results-is-valid? 2 (get-in t-fixture/entries-unnormalized-1 [2 0]))
           (t-common/search-entries t-fixture/entries-unnormalized-1-search-2 10))))
    (finally
      (t-common/finish!))))

;; TODO: Fix ngram analyzer for Lucene 9.x
#_(deftest test-ngram-analyzer-en
  (t-common/prepare! t-common/ngram-analyzer nil t-fixture/entries-en-1)
  (try
    (testing "search exists entries"
      (is (empty? (t-common/search-entries "A" 10)))
      (is ((t-common/results-is-valid? 1 (get-in t-fixture/entries-en-1 [0 0]))
           (t-common/search-entries t-fixture/entries-en-1-search-1 10)))
      (is ((t-common/results-is-valid? 1 (get-in t-fixture/entries-en-1 [0 0]))
           (t-common/search-entries t-fixture/entries-en-1-search-2 10)))
      (is ((t-common/results-is-valid? 1 (get-in t-fixture/entries-en-1 [1 0]))
           (t-common/search-entries t-fixture/entries-en-1-search-3 10)))
      (is ((t-common/results-is-valid? 1 (get-in t-fixture/entries-en-1 [1 0]))
           (t-common/search-entries t-fixture/entries-en-1-search-4 10)))
      (is ((t-common/results-is-valid? 1 (get-in t-fixture/entries-en-1 [2 0]))
           (t-common/search-entries t-fixture/entries-en-1-search-5 10))))
    (testing "search new entries"
      (let [entry-key "9999"
            entry-doc "latest entry"
            search-query "est ent"]
        (is ((t-common/results-is-valid? 1)
             (t-common/search-entries search-query 10)))
        (is (nil? (t-common/add-entry! entry-key entry-doc)))
        (is ((t-common/results-is-valid? 2 entry-key)
             (t-common/search-entries search-query 10)))))
    (finally
      (t-common/finish!))))

;; TODO: Fix ngram analyzer for Lucene 9.x
#_(deftest test-ngram-analyzer-ja
  (t-common/prepare! t-common/ngram-analyzer nil t-fixture/entries-ja-1)
  (try
    (testing "search exists entries"
      (is (empty? (t-common/search-entries "あ" 10)))
      (is ((t-common/results-is-valid? 1 (get-in t-fixture/entries-ja-1 [2 0]))
           (t-common/search-entries t-fixture/entries-ja-1-search-1 10)))
      (is ((t-common/results-is-valid? 1 (get-in t-fixture/entries-ja-1 [2 0]))
           (t-common/search-entries t-fixture/entries-ja-1-search-2 10)))
      (is ((t-common/results-is-valid? 1 (get-in t-fixture/entries-ja-1 [2 0]))
           (t-common/search-entries t-fixture/entries-ja-1-search-3 10)))
      (is ((t-common/results-is-valid? 1 (get-in t-fixture/entries-ja-1 [2 0]))
           (t-common/search-entries t-fixture/entries-ja-1-search-4 10)))
      (is ((t-common/results-is-valid? 1 (get-in t-fixture/entries-ja-1 [1 0]))
           (t-common/search-entries t-fixture/entries-ja-1-search-5 10))))
    (testing "search new entries"
      (let [entry-key "9999"
            entry-doc "最新のエントリ"
            search-query "新のエン"]
        (is ((t-common/results-is-valid? 0)
             (t-common/search-entries search-query 10)))
        (is (nil? (t-common/add-entry! entry-key entry-doc)))
        (is ((t-common/results-is-valid? 1 entry-key)
             (t-common/search-entries search-query 10)))))
    (finally
      (t-common/finish!))))

;; TODO: Fix ngram analyzer for Lucene 9.x
#_(deftest test-ngram-analyzer-phrase-search-en
  (t-common/prepare! t-common/ngram-analyzer nil t-fixture/entries-en-1)
  (try
    (testing "search exists entries"
      (is (empty? (t-common/phrase-search-entries "A" 10)))
      (is ((t-common/results-is-valid? 1 (get-in t-fixture/entries-en-1 [0 0]))
           (t-common/phrase-search-entries t-fixture/entries-en-1-search-1 10)))
      (is ((t-common/results-is-valid? 1 (get-in t-fixture/entries-en-1 [0 0]))
           (t-common/phrase-search-entries t-fixture/entries-en-1-search-2 10)))
      (is ((t-common/results-is-valid? 1 (get-in t-fixture/entries-en-1 [1 0]))
           (t-common/phrase-search-entries t-fixture/entries-en-1-search-3 10)))
      (is ((t-common/results-is-valid? 1 (get-in t-fixture/entries-en-1 [1 0]))
           (t-common/phrase-search-entries t-fixture/entries-en-1-search-4 10)))
      (is ((t-common/results-is-valid? 0)
           (t-common/phrase-search-entries t-fixture/entries-en-1-search-5 10))))
    (testing "search new entries"
      (let [entry-key "9999"
            entry-doc "latest entry"
            search-query "est ent"]
        (is ((t-common/results-is-valid? 0)
             (t-common/phrase-search-entries search-query 10)))
        (is (nil? (t-common/add-entry! entry-key entry-doc)))
        (is ((t-common/results-is-valid? 1 entry-key)
             (t-common/phrase-search-entries search-query 10)))))
    (finally
      (t-common/finish!))))

;; TODO: Fix ngram analyzer for Lucene 9.x
#_(deftest test-ngram-analyzer-phrase-search-ja
  (t-common/prepare! t-common/ngram-analyzer nil t-fixture/entries-ja-1)
  (try
    (testing "search exists entries"
      (is (empty? (t-common/phrase-search-entries "あ" 10)))
      (is ((t-common/results-is-valid? 1 (get-in t-fixture/entries-ja-1 [2 0]))
           (t-common/phrase-search-entries t-fixture/entries-ja-1-search-1 10)))
      (is ((t-common/results-is-valid? 1 (get-in t-fixture/entries-ja-1 [2 0]))
           (t-common/phrase-search-entries t-fixture/entries-ja-1-search-2 10)))
      (is ((t-common/results-is-valid? 1 (get-in t-fixture/entries-ja-1 [2 0]))
           (t-common/phrase-search-entries t-fixture/entries-ja-1-search-3 10)))
      (is ((t-common/results-is-valid? 1 (get-in t-fixture/entries-ja-1 [2 0]))
           (t-common/phrase-search-entries t-fixture/entries-ja-1-search-4 10)))
      (is ((t-common/results-is-valid? 1 (get-in t-fixture/entries-ja-1 [1 0]))
           (t-common/phrase-search-entries t-fixture/entries-ja-1-search-5 10))))
    (testing "search new entries"
      (let [entry-key "9999"
            entry-doc "最新のエントリ"
            search-query "新のエン"]
        (is ((t-common/results-is-valid? 0)
             (t-common/phrase-search-entries search-query 10)))
        (is (nil? (t-common/add-entry! entry-key entry-doc)))
        (is ((t-common/results-is-valid? 1 entry-key)
             (t-common/phrase-search-entries search-query 10)))))
    (finally
      (t-common/finish!))))

(deftest test-cjk-analyzer
  (t-common/prepare! t-common/cjk-analyzer nil t-fixture/entries-ja-1)
  (try
    (testing "search exists entries"
      (is ((t-common/results-is-valid? 1 (get-in t-fixture/entries-ja-1 [2 0]))
           (t-common/search-entries t-fixture/entries-ja-1-search-1 10)))
      (is ((t-common/results-is-valid? 1 (get-in t-fixture/entries-ja-1 [2 0]))
           (t-common/search-entries t-fixture/entries-ja-1-search-2 10)))
      (is ((t-common/results-is-valid? 1 (get-in t-fixture/entries-ja-1 [2 0]))
           (t-common/search-entries t-fixture/entries-ja-1-search-3 10)))
      (is ((t-common/results-is-valid? 1)
           (t-common/search-entries t-fixture/entries-ja-1-search-4 10)))
      (is ((t-common/results-is-valid? 1)
           (t-common/search-entries t-fixture/entries-ja-1-search-5 10))))
    (testing "search new entries"
      (let [entry-key "9999"
            entry-doc "最新のエントリ"
            search-query "新のエン"]
        (is ((t-common/results-is-valid? 0)
             (t-common/search-entries search-query 10)))
        (is (nil? (t-common/add-entry! entry-key entry-doc)))
        (is ((t-common/results-is-valid? 1 entry-key)
             (t-common/search-entries search-query 10)))))
    (finally
      (t-common/finish!))))

(deftest test-kuromoji-analyzer
  (t-common/prepare! t-common/kuromoji-analyzer nil t-fixture/entries-ja-1)
  (try
    (testing "search exists entries"
      (is ((t-common/results-is-valid? 1 (get-in t-fixture/entries-ja-1 [2 0]))
           (t-common/search-entries t-fixture/entries-ja-1-search-1 10)))
      (is ((t-common/results-is-valid? 1 (get-in t-fixture/entries-ja-1 [2 0]))
           (t-common/search-entries t-fixture/entries-ja-1-search-2 10)))
      (is ((t-common/results-is-valid? 0)
           (t-common/search-entries t-fixture/entries-ja-1-search-3 10)))
      (is ((t-common/results-is-valid? 1)
           (t-common/search-entries t-fixture/entries-ja-1-search-4 10)))
      (is ((t-common/results-is-valid? 0)
           (t-common/search-entries t-fixture/entries-ja-1-search-5 10))))
    (testing "search new entries"
      (let [entry-key "9999"
            entry-doc "最新のエントリ"]
        (is ((t-common/results-is-valid? 0)
             (t-common/search-entries entry-doc 10)))
        (is (nil? (t-common/add-entry! entry-key entry-doc)))
        (is ((t-common/results-is-valid? 1 entry-key)
             (t-common/search-entries entry-doc 10)))))
    (finally
      (t-common/finish!))))

(deftest test-with-stop-words
  (testing "standard analyzer"
    (t-common/prepare! (analysis/standard-analyzer t-fixture/stop-words-en) nil t-fixture/entries-en-1)
    (try
      (is ((t-common/results-is-valid? 0)
           (t-common/search-entries "Heo" 10)))
      (finally
        (t-common/finish!))))
  
  ;; TODO: Fix ngram analyzer for Lucene 9.x
  #_(testing "ngram analyzer (en)"
    (t-common/prepare! (analysis/ngram-analyzer 2 8 t-fixture/stop-words-en) nil t-fixture/entries-en-1)
    (try
      (is ((t-common/results-is-valid? 1)
           (t-common/search-entries "Heo" 10)))
      (finally
        (t-common/finish!))))
  
  (testing "cjk analyzer"
    (t-common/prepare! (analysis/cjk-analyzer t-fixture/stop-words-ja) nil t-fixture/entries-ja-1)
    (try
      (is ((t-common/results-is-valid? 1)
           (t-common/search-entries t-fixture/entries-ja-1-search-1 10)))
      (is ((t-common/results-is-valid? 1)
           (t-common/search-entries t-fixture/entries-ja-1-search-2 10)))
      (is ((t-common/results-is-valid? 0)
           (t-common/search-entries t-fixture/entries-ja-1-search-3 10)))
      (is ((t-common/results-is-valid? 1)
           (t-common/search-entries t-fixture/entries-ja-1-search-4 10)))
      (is ((t-common/results-is-valid? 1)
           (t-common/search-entries t-fixture/entries-ja-1-search-5 10)))
      (finally
        (t-common/finish!))))
  
  (testing "kuromoji analyzer"
    (let [ana (analysis/kuromoji-analyzer nil :search t-fixture/stop-words-ja #{})]
      (t-common/prepare! ana nil t-fixture/entries-ja-1)
      (try
        (is ((t-common/results-is-valid? 1)
             (t-common/search-entries t-fixture/entries-ja-1-search-1 10)))
        (is ((t-common/results-is-valid? 1)
             (t-common/search-entries t-fixture/entries-ja-1-search-2 10)))
        (is ((t-common/results-is-valid? 0)
             (t-common/search-entries t-fixture/entries-ja-1-search-3 10)))
        (is ((t-common/results-is-valid? 1)
             (t-common/search-entries t-fixture/entries-ja-1-search-4 10)))
        (is ((t-common/results-is-valid? 0)
             (t-common/search-entries t-fixture/entries-ja-1-search-5 10)))
        (finally
          (t-common/finish!))))))

(deftest test-modify-exists-entries
  (t-common/prepare! t-common/standard-analyzer nil t-fixture/entries-en-1)
  (try
    (testing "update"
      (let [[k doc] (first t-fixture/entries-en-1)
            new-doc "zzz"]
        (is ((t-common/results-is-valid? 1 k)
             (t-common/search-entries doc 10)))
        (is ((t-common/results-is-valid? 0)
             (t-common/search-entries new-doc 10)))
        (is (nil? (t-common/update-entry! k new-doc)))
        (is ((t-common/results-is-valid? 0)
             (t-common/search-entries doc 10)))
        (is ((t-common/results-is-valid? 1 k)
             (t-common/search-entries new-doc 10)))))
    (testing "delete"
      (let [[k doc] (first t-fixture/entries-en-1)]
        (is ((t-common/results-is-valid? 1 k)
             (t-common/search-entries doc 10)))
        (is (nil? (t-common/delete-entry! k)))
        (is ((t-common/results-is-valid? 0)
             (t-common/search-entries doc 10)))))
    (finally
      (t-common/finish!))))

(deftest test-search-utilities
  (t-common/prepare! t-common/standard-analyzer)
  (try
    (testing "use non-string keys"
      (t-common/add-entry! "dummy" "")
      (let [entry-key 55
            entry-doc (str entry-key)]
        (is ((t-common/results-is-valid? 0)
             (t-common/search-entries entry-doc 10)))
        (is (nil? (t-common/add-entry! entry-key "")))
        (is ((t-common/results-is-valid? 0)
             (t-common/search-entries entry-doc 10)))
        (is (nil? (t-common/update-entry! entry-key entry-doc)))
        (is ((t-common/results-is-valid? 1 (str entry-key))
             (t-common/search-entries entry-doc 10)))
        (is (nil? (t-common/delete-entry! entry-key)))
        (is ((t-common/results-is-valid? 0)
             (t-common/search-entries entry-doc 10))))
      (let [entry-key :test
            entry-doc (name entry-key)]
        (is ((t-common/results-is-valid? 0)
             (t-common/search-entries entry-doc 10)))
        (is (nil? (t-common/add-entry! entry-key "")))
        (is ((t-common/results-is-valid? 0)
             (t-common/search-entries entry-doc 10)))
        (is (nil? (t-common/update-entry! entry-key entry-doc)))
        (is ((t-common/results-is-valid? 1 (name entry-key))
             (t-common/search-entries entry-doc 10)))
        (is (nil? (t-common/delete-entry! entry-key)))
        (is ((t-common/results-is-valid? 0)
             (t-common/search-entries entry-doc 10))))
      (let [entry-key (java.util.UUID/randomUUID)
            entry-doc (str entry-key)]
        (is ((t-common/results-is-valid? 0)
             (t-common/search-entries entry-doc 10)))
        (is (nil? (t-common/add-entry! entry-key "")))
        (is ((t-common/results-is-valid? 0)
             (t-common/search-entries entry-doc 10)))
        (is (nil? (t-common/update-entry! entry-key entry-doc)))
        (is ((t-common/results-is-valid? 1 (str entry-key))
             (t-common/search-entries entry-doc 10)))
        (is (nil? (t-common/delete-entry! entry-key)))
        (is ((t-common/results-is-valid? 0)
             (t-common/search-entries entry-doc 10)))))
    (testing "with pagination"
      (let [doc-prefix "with pagination"]
        (dotimes [i 105]
          (let [index (+ i 50000)
                doc (str doc-prefix index)]
            (t-common/add-entry! (str index) doc)))
        (is ((t-common/results-is-valid? 10)
             (t-common/search-entries doc-prefix 100 0 10)))
        (is ((t-common/results-is-valid? 9)
             (t-common/search-entries doc-prefix 19 1 10)))
        (is ((t-common/results-is-valid? 5)
             (t-common/search-entries doc-prefix 200 10 10)))
        (is ((t-common/results-is-valid? 105)
             (t-common/search-entries doc-prefix 200 0 200)))
        (is ((t-common/results-is-valid? 0)
             (t-common/search-entries doc-prefix 200 200 200)))
        (is ((t-common/results-is-valid? 0)
             (t-common/search-entries doc-prefix 5 200 5)))
        (is ((t-common/results-is-valid? 8)
             (t-common/search-entries doc-prefix 8 0 200)))))
    (finally
      (t-common/finish!))))

(run-testset! t-fixture/testset-kw-s)
(run-testset! t-fixture/testset-kw-w)
(run-testset! t-fixture/testset-kw-q)
(run-testset! t-fixture/testset-std-s)
(run-testset! t-fixture/testset-std-p)
(run-testset! t-fixture/testset-std-q)
(run-testset! t-fixture/testset-cjk-s)
(run-testset! t-fixture/testset-cjk-p)
(run-testset! t-fixture/testset-cjk-q)
(run-testset! t-fixture/testset-kuro-s)
(run-testset! t-fixture/testset-kuro-p)
(run-testset! t-fixture/testset-kuro-q)
;; TODO: Fix ngram analyzer for Lucene 9.x
#_(run-testset! t-fixture/testset-ngram-s)
#_(run-testset! t-fixture/testset-ngram-p)
#_(run-testset! t-fixture/testset-ngram-q)

(deftest test-raw-query-search
  (t-common/prepare! t-common/standard-analyzer nil t-fixture/entries-en-1)
  (try
    (letfn [(parse-query [^String s]
              (.parse (StandardQueryParser. t-common/standard-analyzer) s "doc"))]
      (testing "raw query search"
        (is ((t-common/results-is-valid? 1)
             (core/search @t-common/test-store (parse-query "doc:20130819") 10)))
        (is ((t-common/results-is-valid? 2)
             (core/search @t-common/test-store (parse-query "Hello Tokyo") 10)))
        (is ((t-common/results-is-valid? 0)
             (core/search @t-common/test-store (parse-query "Hello AND Tokyo") 10)))))
    (finally
      (t-common/finish!))))

(deftest test-abnormal-usage
  (t-common/prepare! nil nil t-fixture/entries-en-1)
  (try
    (testing "manipulate with default analyzer"
      (let [[k doc] (first t-fixture/entries-en-1)
            new-doc "newer"]
        (is ((t-common/results-is-valid? 1 k)
             (t-common/search-entries doc 10)))
        (core/update! @t-common/test-store
                      {:key k, :doc new-doc}
                      [:key :doc]
                      :key k)
        (is ((t-common/results-is-valid? 0)
             (t-common/search-entries doc 10)))
        (is ((t-common/results-is-valid? 1 k)
             (t-common/search-entries new-doc 10)))
        (core/delete! @t-common/test-store :key k)
        (is ((t-common/results-is-valid? 0)
             (t-common/search-entries doc 10)))
        (is ((t-common/results-is-valid? 0)
             (t-common/search-entries new-doc 10)))))
    (testing "without index"
      (let [k "new-key"
            doc "this_is_new_entry"]
        (core/add! @t-common/test-store [{:key k, :doc doc}] [])
        (is ((t-common/results-is-valid? 0)
             (t-common/search-entries doc 10)))))
    (finally
      (t-common/finish!))))

(deftest test-disk-store
  (let [tmp-store-path (t-common/get-tmp-dir)
        k "123"
        doc "abc"]
    (t-common/prepare! t-common/standard-analyzer tmp-store-path)
    (try
      (testing "disk store"
        (t-common/add-entry! "dummy" "")
        (is ((t-common/results-is-valid? 0)
             (t-common/search-entries doc 10)))
        (is (nil? (t-common/add-entry! k doc)))
        (is ((t-common/results-is-valid? 1 k)
             (t-common/search-entries doc 10)))
        ;; Switch to another session
        (store/close! @t-common/test-store)
        (reset! t-common/test-store (store/disk-store tmp-store-path))
        (is ((t-common/results-is-valid? 1 k)
             (t-common/search-entries doc 10))))
      (finally
        (t-common/finish! tmp-store-path)))))

(deftest test-add!
  (t-common/prepare! (analysis/standard-analyzer) nil t-fixture/entries-en-1)
  (try
    (testing "with store"
      (is (nil? (core/add! @t-common/test-store [{:key "123", :doc "abc"}] [:key :doc])))
      (is (nil? (core/add! @t-common/test-store [{:key "456", :doc "def"}] [:key :doc]
                           (analysis/standard-analyzer)))))
    (testing "with writer"
      (with-open [writer (store/store-writer @t-common/test-store (analysis/standard-analyzer))]
        (is (nil? (core/add! writer [{:key "123", :doc "abc"}] [:key :doc])))
        (is (thrown? clojure.lang.ArityException
                     (core/add! writer [{:key "456", :doc "def"}] [:key :doc]
                                (analysis/standard-analyzer))))))
    (finally
      (t-common/finish!))))

(deftest test-update!
  (t-common/prepare! (analysis/standard-analyzer) nil t-fixture/entries-en-1)
  (try
    (testing "with store"
      (is (nil? (core/update! @t-common/test-store {:key "2", :doc "Hello world"} [:key :doc] :key "2")))
      (is (nil? (core/update! @t-common/test-store {:key "3", :doc "Osaka station"} [:key :doc] :key "3"
                              (analysis/standard-analyzer)))))
    (testing "with writer"
      (with-open [writer (store/store-writer @t-common/test-store (analysis/standard-analyzer))]
        (is (nil? (core/update! writer {:key "2", :doc "Hello world"} [:key :doc] :key "2")))
        (is (thrown? clojure.lang.ArityException
                     (core/update! writer {:key "3", :doc "Osaka station"} [:key :doc] :key "3"
                                   (analysis/standard-analyzer))))))
    (finally
      (t-common/finish!))))

(deftest test-delete!
  (t-common/prepare! (analysis/standard-analyzer) nil t-fixture/entries-en-1)
  (try
    (testing "with store"
      (is (nil? (core/delete! @t-common/test-store :key "1")))
      (is (nil? (core/delete! @t-common/test-store :key "2" (analysis/standard-analyzer)))))
    (testing "with writer"
      (with-open [writer (store/store-writer @t-common/test-store (analysis/standard-analyzer))]
        (is (nil? (core/delete! writer :key "1")))
        (is (thrown? clojure.lang.ArityException
                     (core/delete! writer :key "2" (analysis/standard-analyzer))))))
    (finally
      (t-common/finish!))))

(deftest test-search*
  (t-common/prepare! (analysis/standard-analyzer) nil t-fixture/entries-en-1)
  (try
    (testing "with store"
      (is ((t-common/results-is-valid? 1 (get-in t-fixture/entries-en-1 [0 0]))
           (#'core/search* :query @t-common/test-store
                           [{:doc t-fixture/entries-en-1-search-1}]
                           10 (analysis/standard-analyzer) 0 10))))
    (testing "with reader"
      (with-open [reader (store/store-reader @t-common/test-store)]
        (is ((t-common/results-is-valid? 1 (get-in t-fixture/entries-en-1 [0 0]))
             (#'core/search* :query reader
                             [{:doc t-fixture/entries-en-1-search-1}]
                             10 (analysis/standard-analyzer) 0 10)))))
    (finally
      (t-common/finish!))))
