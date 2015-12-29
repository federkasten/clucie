(ns clucie.t-core
  (:require [midje.sweet :refer :all]
            [clucie.core :as core]
            [clucie.analysis :as analysis]
            [clucie.store :as store]
            [clucie.t-common :as t-common]
            [clucie.t-fixture :as t-fixture])
  (:import [java.util UUID]))

(facts "standard analyzer"
  (with-state-changes [(before :facts (t-common/prepare! t-common/standard-analyzer nil t-fixture/entries-en-1))
                       (after :facts (t-common/finish!))]
    (fact "search exists entries"
      (t-common/search-entries t-fixture/entries-en-1-search-1 10) => (t-common/results-is-valid? 1 (get-in t-fixture/entries-en-1 [0 0]))
      (t-common/search-entries t-fixture/entries-en-1-search-2 10) => (t-common/results-is-valid? 0)
      (t-common/search-entries t-fixture/entries-en-1-search-3 10) => (t-common/results-is-valid? 1 (get-in t-fixture/entries-en-1 [1 0]))
      (t-common/search-entries t-fixture/entries-en-1-search-4 10) => (t-common/results-is-valid? 0))
    (fact "search new entries"
      (let [entry-key "9999"
            entry-doc "latest entry"]
        (t-common/search-entries entry-doc 10) => (t-common/results-is-valid? 0)
        (t-common/add-entry! entry-key entry-doc) => nil
        (t-common/search-entries entry-doc 10) => (t-common/results-is-valid? 1 entry-key)))))

(facts "ngram analyzer"
  (with-state-changes [(before :facts (t-common/prepare! t-common/ngram-analyzer nil t-fixture/entries-en-1))
                       (after :facts (t-common/finish!))]
    (fact "search exists entries"
      (t-common/search-entries t-fixture/entries-en-1-search-1 10) => (t-common/results-is-valid? 1 (get-in t-fixture/entries-en-1 [0 0]))
      (t-common/search-entries t-fixture/entries-en-1-search-2 10) => (t-common/results-is-valid? 1 (get-in t-fixture/entries-en-1 [0 0]))
      (t-common/search-entries t-fixture/entries-en-1-search-3 10) => (t-common/results-is-valid? 1 (get-in t-fixture/entries-en-1 [1 0]))
      (t-common/search-entries t-fixture/entries-en-1-search-4 10) => (t-common/results-is-valid? 1 (get-in t-fixture/entries-en-1 [1 0])))
    (fact "search new entries"
      (let [entry-key "9999"
            entry-doc "latest entry"]
        (t-common/search-entries entry-doc 10) => (t-common/results-is-valid? 1)
        (t-common/add-entry! entry-key entry-doc) => nil
        (t-common/search-entries entry-doc 10) => (t-common/results-is-valid? 2 entry-key)))))

(facts "cjk analyzer"
  (with-state-changes [(before :facts (t-common/prepare! t-common/cjk-analyzer nil t-fixture/entries-ja-1))
                       (after :facts (t-common/finish!))]
    (fact "search exists entries"
      (t-common/search-entries t-fixture/entries-ja-1-search-1 10) => (t-common/results-is-valid? 1 (get-in t-fixture/entries-ja-1 [2 0]))
      (t-common/search-entries t-fixture/entries-ja-1-search-2 10) => (t-common/results-is-valid? 1 (get-in t-fixture/entries-ja-1 [2 0]))
      (t-common/search-entries t-fixture/entries-ja-1-search-3 10) => (t-common/results-is-valid? 1 (get-in t-fixture/entries-ja-1 [2 0]))
      (t-common/search-entries t-fixture/entries-ja-1-search-4 10) => (t-common/results-is-valid? 1))
    (fact "search new entries"
      (let [entry-key "9999"
            entry-doc "latest entry"]
        (t-common/search-entries entry-doc 10) => (t-common/results-is-valid? 0)
        (t-common/add-entry! entry-key entry-doc) => nil
        (t-common/search-entries entry-doc 10) => (t-common/results-is-valid? 1 entry-key)))))

(facts "kuromoji analyzer"
  (with-state-changes [(before :facts (t-common/prepare! t-common/kuromoji-analyzer nil t-fixture/entries-ja-1))
                       (after :facts (t-common/finish!))]
    (fact "search exists entries"
      (t-common/search-entries t-fixture/entries-ja-1-search-1 10) => (t-common/results-is-valid? 1 (get-in t-fixture/entries-ja-1 [2 0]))
      (t-common/search-entries t-fixture/entries-ja-1-search-2 10) => (t-common/results-is-valid? 1 (get-in t-fixture/entries-ja-1 [2 0]))
      (t-common/search-entries t-fixture/entries-ja-1-search-3 10) => (t-common/results-is-valid? 0)
      (t-common/search-entries t-fixture/entries-ja-1-search-4 10) => (t-common/results-is-valid? 1))
    (fact "search new entries"
      (let [entry-key "9999"
            entry-doc "latest entry"]
        (t-common/search-entries entry-doc 10) => (t-common/results-is-valid? 0)
        (t-common/add-entry! entry-key entry-doc) => nil
        (t-common/search-entries entry-doc 10) => (t-common/results-is-valid? 1 entry-key)))))

(facts "with stop-words"
  (with-state-changes [(before :facts (t-common/prepare! (analysis/standard-analyzer t-fixture/stop-words-en) nil t-fixture/entries-en-1))
                       (after :facts (t-common/finish!))]
    (fact "standard analyzer"
      (t-common/search-entries "Hello" 10) => (t-common/results-is-valid? 1)))
  (with-state-changes [(before :facts (t-common/prepare! (analysis/cjk-analyzer t-fixture/stop-words-ja) nil t-fixture/entries-ja-1))
                       (after :facts (t-common/finish!))]
    (fact "cjk analyzer"
      (t-common/search-entries t-fixture/entries-ja-1-search-1 10) => (t-common/results-is-valid? 1)
      (t-common/search-entries t-fixture/entries-ja-1-search-2 10) => (t-common/results-is-valid? 1)
      (t-common/search-entries t-fixture/entries-ja-1-search-3 10) => (t-common/results-is-valid? 0)
      (t-common/search-entries t-fixture/entries-ja-1-search-4 10) => (t-common/results-is-valid? 1)))
  (with-state-changes [(before :facts (let [ana (analysis/kuromoji-analyzer nil :search t-fixture/stop-words-ja #{})]
                                        (t-common/prepare! ana nil t-fixture/entries-ja-1)))
                       (after :facts (t-common/finish!))]
    (fact "kuromoji analyzer"
      (t-common/search-entries t-fixture/entries-ja-1-search-1 10) => (t-common/results-is-valid? 1)
      (t-common/search-entries t-fixture/entries-ja-1-search-2 10) => (t-common/results-is-valid? 1)
      (t-common/search-entries t-fixture/entries-ja-1-search-3 10) => (t-common/results-is-valid? 0)
      (t-common/search-entries t-fixture/entries-ja-1-search-4 10) => (t-common/results-is-valid? 1))))

(facts "modify exists entries"
  (with-state-changes [(before :facts (t-common/prepare! t-common/standard-analyzer nil t-fixture/entries-en-1))
                       (after :facts (t-common/finish!))]
    (fact "update"
      (let [[k doc] (first t-fixture/entries-en-1)
            new-doc "zzz"]
        (t-common/search-entries doc 10) => (t-common/results-is-valid? 1 k)
        (t-common/search-entries new-doc 10) => (t-common/results-is-valid? 0)
        (t-common/update-entry! k new-doc) => nil
        (t-common/search-entries doc 10) => (t-common/results-is-valid? 0)
        (t-common/search-entries new-doc 10) => (t-common/results-is-valid? 1 k)))
    (fact "delete"
      (let [[k doc] (first t-fixture/entries-en-1)]
        (t-common/search-entries doc 10) => (t-common/results-is-valid? 1 k)
        (t-common/delete-entry! k) => nil
        (t-common/search-entries doc 10) => (t-common/results-is-valid? 0)))))

(facts "search utilities"
  (with-state-changes [(before :facts (t-common/prepare! t-common/standard-analyzer))
                       (after :facts (t-common/finish!))]
    (fact "use non-string keys"
      (t-common/add-entry! "dummy" "")
      (let [entry-key 55
            entry-doc (str entry-key)]
        (t-common/search-entries entry-doc 10) => (t-common/results-is-valid? 0)
        (t-common/add-entry! entry-key "") => nil
        (t-common/search-entries entry-doc 10) => (t-common/results-is-valid? 0)
        (t-common/update-entry! entry-key entry-doc) => nil
        (t-common/search-entries entry-doc 10) => (t-common/results-is-valid? 1 (str entry-key))
        (t-common/delete-entry! entry-key) => nil
        (t-common/search-entries entry-doc 10) => (t-common/results-is-valid? 0))
      (let [entry-key :test
            entry-doc (name entry-key)]
        (t-common/search-entries entry-doc 10) => (t-common/results-is-valid? 0)
        (t-common/add-entry! entry-key "") => nil
        (t-common/search-entries entry-doc 10) => (t-common/results-is-valid? 0)
        (t-common/update-entry! entry-key entry-doc) => nil
        (t-common/search-entries entry-doc 10) => (t-common/results-is-valid? 1 (name entry-key))
        (t-common/delete-entry! entry-key) => nil
        (t-common/search-entries entry-doc 10) => (t-common/results-is-valid? 0))
      (let [entry-key (java.util.UUID/randomUUID)
            entry-doc (str entry-key)]
        (t-common/search-entries entry-doc 10) => (t-common/results-is-valid? 0)
        (t-common/add-entry! entry-key "") => nil
        (t-common/search-entries entry-doc 10) => (t-common/results-is-valid? 0)
        (t-common/update-entry! entry-key entry-doc) => nil
        (t-common/search-entries entry-doc 10) => (t-common/results-is-valid? 1 (str entry-key))
        (t-common/delete-entry! entry-key) => nil
        (t-common/search-entries entry-doc 10) => (t-common/results-is-valid? 0)))
    (fact "with pagination"
      (let [doc-prefix "with pagination"]
        (dotimes [i 105]
          (let [index (+ i 50000)
                doc (str doc-prefix index)]
            (t-common/add-entry! (str index) doc)))
        (t-common/search-entries doc-prefix 100 0 10) => (t-common/results-is-valid? 10)
        (t-common/search-entries doc-prefix 19 1 10) => (t-common/results-is-valid? 9)
        (t-common/search-entries doc-prefix 200 10 10) => (t-common/results-is-valid? 5)
        (t-common/search-entries doc-prefix 200 0 200) => (t-common/results-is-valid? 105)
        (t-common/search-entries doc-prefix 200 200 200) => (t-common/results-is-valid? 0)
        (t-common/search-entries doc-prefix 5 200 5) => (t-common/results-is-valid? 0)
        (t-common/search-entries doc-prefix 8 0 200) => (t-common/results-is-valid? 8)))))

(facts "abnormal usage"
  (with-state-changes [(before :facts (t-common/prepare! nil nil t-fixture/entries-en-1))
                       (after :facts (t-common/finish!))]
    (fact "manipulate with default analyzer"
      (let [[k doc] (first t-fixture/entries-en-1)
            new-doc "newer"]
        (t-common/search-entries doc 10) => (t-common/results-is-valid? 1 k)
        (core/update! @t-common/test-store
                      {:key k, :doc new-doc}
                      [:key :doc]
                      :key k)
        (t-common/search-entries doc 10) => (t-common/results-is-valid? 0)
        (t-common/search-entries new-doc 10) => (t-common/results-is-valid? 1 k)
        (core/delete! @t-common/test-store :key k)
        (t-common/search-entries doc 10) => (t-common/results-is-valid? 0)
        (t-common/search-entries new-doc 10) => (t-common/results-is-valid? 0)))
    (fact "without index"
      (let [k "new-key"
            doc "this_is_new_entry"]
        (core/add! @t-common/test-store [{:key k, :doc doc}] [])
        (t-common/search-entries doc 10) => (t-common/results-is-valid? 0)))))

(let [tmp-store-path (t-common/get-tmp-dir)
      k "123"
      doc "abc"]
  (with-state-changes [(before :facts (t-common/prepare! t-common/standard-analyzer tmp-store-path))
                       (after :facts (t-common/finish! tmp-store-path))]
    (fact "disk store"
      (t-common/add-entry! "dummy" "")
      (t-common/search-entries doc 10) => (t-common/results-is-valid? 0)
      (t-common/add-entry! k doc) => nil
      (t-common/search-entries doc 10) => (t-common/results-is-valid? 1 k)
      ;; Switch to another session
      (store/close! @t-common/test-store)
      (reset! t-common/test-store (store/disk-store tmp-store-path))
      (t-common/search-entries doc 10) => (t-common/results-is-valid? 1 k))))
