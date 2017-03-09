# Clucie

Clojure for the Lucene

[![Build Status](https://travis-ci.org/federkasten/clucie.svg)](https://travis-ci.org/federkasten/clucie)

[![Dependency Status](https://www.versioneye.com/user/projects/568202c5eb4f47003c0009b3/badge.svg?style=flat)](https://www.versioneye.com/user/projects/568202c5eb4f47003c0009b3)

[![Clojars Project](https://img.shields.io/clojars/v/clucie.svg)](https://clojars.org/clucie)

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

;; OR search
(core/search index-store
             {:title #{"Beatles" "Please"}}
             10
             analyzer
             0
             5)

;; => [{:number "1", :title "Please Please Me"} {:number "2", :title "With the Beatles"} {:number "4", :title "Beatles for Sale"}]

;; Get score
(let [results (core/search index-store
                           {:title #{"Beatles" "Please"}}
                           10
                           analyzer
                           0
                           5)]
  (map #(:score (meta %)) results))

;; => (0.62241787 0.3930676 0.3930676)

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
                                            {:content clj-analyzer}))

(core/add! index-store
           [{:key "English" :content "Thank you"}
            {:key "Chinese" :content "谢谢"}
            {:key "Japanese" :content "ありがとう"}
            {:key "Korean" :content "고마워요"}]
           [:key :content]
           analyzer)
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

# Run tests

Run `lein midje`.

# Get coverage

Run `lein cloverage` and see `target/coverage/index.html`.

# License

Copyright [Takashi AOKI][tak.sh] and other contributors.

Licensed under the [Apache License, Version 2.0][apache-license-2.0].

[tak.sh]: http://tak.sh
[apache-license-2.0]: http://www.apache.org/licenses/LICENSE-2.0.html
