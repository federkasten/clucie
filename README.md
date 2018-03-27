# Clucie

Clojure for the Lucene

[![Build Status](https://travis-ci.org/federkasten/clucie.svg)](https://travis-ci.org/federkasten/clucie)

[![Clojars Project](https://img.shields.io/clojars/v/clucie.svg)](https://clojars.org/clucie)

[![codecov](https://codecov.io/gh/federkasten/clucie/branch/master/graph/badge.svg)](https://codecov.io/gh/federkasten/clucie)

# Usage

## Simple Usage

```clojure
(require '[clucie.core :as core])
(require '[clucie.analysis :as analysis])
(require '[clucie.store :as store])

(def analyzer (analysis/standard-analyzer))
(def index-store (store/memory-store)) ; or (store/disk-store "path/to/store")

(core/add! index-store
           [{:number "1" :title "Please Please Me"}
            {:number "2" :title "With the Beatles"}
            {:number "3" :title "A Hard Day's Night"}
            {:number "4" :title "Beatles for Sale"}
            {:number "5" :title "Help!"}]
           [:number :title]
           analyzer)

(core/search index-store
             {:title "Beatles"}
             10 ; max-num
             analyzer
             0 ; page
             5) ; max-num-per-page

;; => [{:number "2", :title "With the Beatles"} {:number "4", :title "Beatles for Sale"}]

;; Phrase search
(core/phrase-search index-store
                    {:title "beatles for"}
                    10
                    analyzer
                    0
                    5)

;; => [{:number "4", :title "Beatles for Sale"}]

(core/phrase-search index-store
                    {:title "for beatles"}
                    10
                    analyzer
                    0
                    5)

;; => []

;; AND search
(core/search index-store
             {:title ["Beatles" "Sale"]}
             10
             analyzer
             0
             5)

;; => [{:number "4", :title "Beatles for Sale"}]

;; AND search, across multiple keys
(core/search index-store
             [{:number "4"} {:title ["Beatles" "Sale"]}]
             10
             analyzer
             0
             5)

;; => [{:number "4", :title "Beatles for Sale"}]

(core/search index-store
             [{:number "3"} {:title "Beatles"}]
             10
             analyzer
             0
             5)

;; => []

;; OR search
(core/search index-store
             {:title #{"Beatles" "Please"}}
             10
             analyzer
             0
             5)

;; => [{:number "1", :title "Please Please Me"} {:number "2", :title "With the Beatles"} {:number "4", :title "Beatles for Sale"}]

;; Get meta information
(let [results (core/search index-store
                           {:title #{"Beatles" "Please"}}
                           10
                           analyzer
                           0
                           5)]
  ;; the total number of hits
  (prn (:total-hits (meta results))) ; => 3
  ;; scores
  (prn (map #(:score (meta %)) results))) ; => (0.62241787 0.3930676 0.3930676)

(store/close! index-store)
```

To update index,

```clojure
(core/update! index-store
              {:number "5" :title "Help! (1965)"}
              [:number :title]
              :number "5"
              analyzer)
```

To delete index,

```clojure
(core/delete! index-store :number "5" analyzer)
```

## CJK (Chinese, Japanese, and Korean) Support

```clojure
(def cjk-analyzer (analysis/cjk-analyzer))

(def my-analyzer (analysis/analyzer-mapping (analysis/keyword-analyzer)
                                            {:content cjk-analyzer}))

(core/add! index-store
           [{:key "English" :content "Thank you"}
            {:key "Chinese" :content "谢谢"}
            {:key "Japanese" :content "ありがとう"}
            {:key "Korean" :content "고마워요"}]
           [:key :content]
           my-analyzer)
```

## Japanese Support (Kuromoji)

```clojure
(def kuromoji-analyzer (analysis/kuromoji-analyzer))

(def my-analyzer (analysis/analyzer-mapping (analysis/keyword-analyzer)
                                            {:content kuromoji-analyzer}))
```

To tokenize,

```clojure
(let [text "富士は日本一の山"
      user-dict nil
      discard-punctuation? true
      mode :normal ; :normal :extended :search
      factory nil]
  (analysis/kuromoji-tokenize text user-dict discard-punctuation? mode factory)) ; => ("富士" "は" "日本一" "の" "山")
```

## Custom analyzer

To build custom analyzer, you can use `build-analyzer` macro.
The following example builds an analyzer that normalizes input texts, splits texts into words, and generates n-grams.

```clojure
(analysis/build-analyzer
  (JapaneseTokenizer. nil true JapaneseTokenizer$Mode/NORMAL)
  :char-filter-factories [(ICUNormalizer2CharFilterFactory. (HashMap. {"name" "nfkc", "mode" "compose"}))]
  :token-filters [(LowerCaseFilter.)
                  (max-shingle/MaxShingleFilter. 3 " ")])
```

## Reusing connections

By default, update/search functions create a new writer/reader each time,
however, that is somewhat inefficient and not thread-safe. For high performance
or concurrent processing, you can pass directly a writer/reader to them.

```clojure
(with-open [writer (store/store-writer index-store analyzer)]
  (core/add! writer
             [{:number "1" :title "Please Please Me"}
              {:number "2" :title "With the Beatles"}]
             [:number :title]))

(with-open [reader (store/store-reader index-store)]
  (core/search reader
               {:title "Beatles"}
               10
               analyzer))
```

# Run tests

Run `lein midje`.

# Get coverage

Run `lein cloverage` and see `target/coverage/index.html`.

# License

Copyright [Takashi AOKI][tak.sh] and other contributors.

Licensed under the [Apache License, Version 2.0][apache-license-2.0].

[tak.sh]: http://tak.sh
[apache-license-2.0]: http://www.apache.org/licenses/LICENSE-2.0.html
