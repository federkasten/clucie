(ns clucie.t-fixture
  (:require [midje.sweet :refer :all]
            [clucie.core :as core]
            [clucie.analysis :as analysis]
            [clucie.t-common :as t-common])
  (:import [org.apache.lucene.queryparser.classic ParseException]))

(def entries-en-1
  [["1" "20130819"]
   ["2" "Hello"]
   ["3" "Tokyo station"]])

(def stop-words-en ["ll"])

(def entries-en-1-search-1 "20130819")
(def entries-en-1-search-2 "2013")
(def entries-en-1-search-3 "hello")
(def entries-en-1-search-4 "lo")
(def entries-en-1-search-5 "ton")

(def entries-en-1-search-wildcard-1 "20130819")
(def entries-en-1-search-wildcard-2 "2013*")
(def entries-en-1-search-wildcard-3 "hello")
(def entries-en-1-search-wildcard-4 "*lo*")

(def entries-ja-1
  [["11" "20130819"]
   ["12" "こんにちは"]
   ["13" "東京都庁"]])

(def stop-words-ja ["京都"])

(def entries-ja-1-search-1 "東京都庁")
(def entries-ja-1-search-2 "東京")
(def entries-ja-1-search-3 "京都")
(def entries-ja-1-search-4 "都庁")
(def entries-ja-1-search-5 "にち")

(def tokenize-wo-optargs-text "東京都庁")
(def tokenize-wo-optargs-result ["東京" "都庁"])

(def tokenize-w-optargs-text
  "コンニチハ、あれは富士山ですか？")
(def tokenize-w-optargs-false-normal
  ["コンニチハ" "、" "あれ" "は" "富士山" "です" "か" "？"])
(def tokenize-w-optargs-false-extended
  ["コ" "ン" "ニ" "チ" "ハ" "、" "あれ" "は" "富士" "山" "です" "か" "？"])
(def tokenize-w-optargs-false-search
  ["コンニチハ" "、" "あれ" "は" "富士" "富士山" "山" "です" "か" "？"])
(def tokenize-w-optargs-true-normal
  ["コンニチハ" "あれ" "は" "富士山" "です" "か"])
(def tokenize-w-optargs-true-extended
  ["コ" "ン" "ニ" "チ" "ハ" "あれ" "は" "富士" "山" "です" "か"])
(def tokenize-w-optargs-true-search
  ["コンニチハ" "あれ" "は" "富士" "富士山" "山" "です" "か"])

;;; newer datasets and testsets

(def dataset-1
  {"1" "20130819"
   "2" "Hello"
   "3" "Tokyo station"
   "11" "20130820"
   "12" "こんにちは"
   "13" "東京都庁"
   "14" "バナナ"})

(def testset-kw-s
  {:testname "testset-kw-s"
   :testdesc "keyword + search"
   :dataset dataset-1
   :analyzer t-common/keyword-analyzer
   :search-fn core/search
   :query+result {"20130819" #{"1"}
                  "2013" #{}
                  "Hello" #{"2"}
                  "hello" #{}
                  "lo" #{}
                  "ton" #{}
                  "東京都庁" #{"13"}
                  "東京" #{}
                  "京都" #{}
                  "都庁" #{}
                  "にち" #{}
                  "バナ ナナ" #{}}})

(def testset-kw-w
  {:testname "testset-kw-w"
   :testdesc "keyword + wildcard-search"
   :dataset dataset-1
   :analyzer t-common/keyword-analyzer
   :search-fn core/wildcard-search
   :query+result {"20130819" #{"1"}
                  "2013*" #{"1" "11"}
                  "Hello" #{"2"}
                  "hello" #{}
                  "*lo*" #{"2"}
                  "*ton*" #{}
                  "東京都庁" #{"13"}
                  "東京" #{}
                  "京都" #{}
                  "都庁" #{}
                  "*にち*" #{"12"}
                  "バナ ナナ" #{}}})

(def testset-kw-q
  {:testname "testset-kw-q"
   :testdesc "keyword + qp-search"
   :dataset dataset-1
   :analyzer t-common/keyword-analyzer
   :search-fn core/qp-search
   :query+result {"20130819" #{"1"}
                  "2013*" #{"1" "11"}
                  "Hello" #{"2"}
                  "\"Hello\"" #{"2"}
                  ;; Classical QueryParser doesn't support
                  ;; first wildcard character in query string.
                  ;; (But WildcardQuery supported first wildcard character)
                  ;; This may fix by lucene developer team in future.
                  "*lo*" ParseException
                  "*ton*" ParseException
                  ;; "*lo*" #{"2"}
                  ;; "*ton*" #{}
                  "key:12" #{"12"}
                  "東京?庁" #{"13"}
                  "/こん(にち|ばん)は/" #{"12"}
                  "東京~" #{"13"}
                  "[20130000 TO 20140000]" #{"1" "11"}
                  "Tokyo AND station" #{}
                  "[[[:::/~~" ParseException
                  "バナ ナナ" #{}}})

(def testset-std-s
  {:testname "testset-std-s"
   :testdesc "standard + search"
   :dataset dataset-1
   :analyzer t-common/standard-analyzer
   :search-fn core/search
   :query+result {"20130819" #{"1"}
                  "2013" #{}
                  "hello" #{"2"}
                  "lo" #{}
                  "ton" #{}
                  "東京都庁" #{"13"}
                  "東京" #{"13"}
                  "京都" #{"13"}
                  "都庁" #{"13"}
                  "にち" #{"12"}
                  "バナ ナナ" #{}}})

(def testset-std-p
  {:testname "testset-std-p"
   :testdesc "standard + phrase-search"
   :dataset dataset-1
   :analyzer t-common/standard-analyzer
   :search-fn core/phrase-search
   :query+result {"20130819" #{"1"}
                  "2013" #{}
                  "hello" #{"2"}
                  "lo" #{}
                  "ton" #{}
                  "東京都庁" #{"13"}
                  "東京" #{"13"}
                  "京都" #{"13"}
                  "都庁" #{"13"}
                  "にち" #{"12"}
                  "バナ ナナ" #{}}})

(def testset-std-q
  {:testname "testset-std-q"
   :testdesc "standard + qp-search"
   :dataset dataset-1
   :analyzer t-common/standard-analyzer
   :search-fn core/qp-search
   :query+result {"20130819" #{"1"}
                  "2013*" #{"1" "11"}
                  "Hello" #{"2"}
                  "\"Hello\"" #{"2"}
                  "key:12" #{"12"}
                  "東京?庁" #{}
                  "/こん(にち|ばん)は/" #{}
                  "東京~" #{"12" "13"} ; ???
                  "[20130000 TO 20140000]" #{"1" "11"}
                  "Tokyo AND station" #{"3"}
                  "バナ ナナ" #{}}})

(def testset-cjk-s
  {:testname "testset-cjk-s"
   :testdesc "cjk + search"
   :dataset dataset-1
   :analyzer t-common/cjk-analyzer
   :search-fn core/search
   :query+result {"20130819" #{"1"}
                  "2013" #{}
                  "hello" #{"2"}
                  "lo" #{}
                  "ton" #{}
                  "東京都庁" #{"13"}
                  "東京" #{"13"}
                  "京都" #{"13"}
                  "都庁" #{"13"}
                  "にち" #{"12"}
                  "バナ ナナ" #{"14"}}})

(def testset-cjk-p
  {:testname "testset-cjk-p"
   :testdesc "cjk + phrase-search"
   :dataset dataset-1
   :analyzer t-common/cjk-analyzer
   :search-fn core/phrase-search
   :query+result {"20130819" #{"1"}
                  "2013" #{}
                  "hello" #{"2"}
                  "lo" #{}
                  "ton" #{}
                  "東京都庁" #{"13"}
                  "東京" #{"13"}
                  "京都" #{"13"}
                  "都庁" #{"13"}
                  "にち" #{"12"}
                  "バナ ナナ" #{"14"}}})

(def testset-cjk-q
  {:testname "testset-cjk-q"
   :testdesc "cjk + qp-search"
   :dataset dataset-1
   :analyzer t-common/cjk-analyzer
   :search-fn core/qp-search
   :query+result {"20130819" #{"1"}
                  "2013*" #{"1" "11"}
                  "Hello" #{"2"}
                  "\"Hello\"" #{"2"}
                  "key:12" #{"12"}
                  "東京?庁" #{}
                  "/こん(にち|ばん)は/" #{}
                  "東京~" #{"12" "13" "14"} ; ???
                  "[20130000 TO 20140000]" #{"1" "11"}
                  "Tokyo AND station" #{"3"}
                  "バナ ナナ" #{"14"}}})

(def testset-kuro-s
  {:testname "testset-kuro-s"
   :testdesc "kuromoji + search"
   :dataset dataset-1
   :analyzer t-common/kuromoji-analyzer
   :search-fn core/search
   :query+result {"20130819" #{"1"}
                  "2013" #{}
                  "hello" #{"2"}
                  "lo" #{}
                  "ton" #{}
                  "東京都庁" #{"13"}
                  "東京" #{"13"}
                  "京都" #{}
                  "都庁" #{"13"}
                  "にち" #{}
                  "バナ ナナ" #{}}})

(def testset-kuro-p
  {:testname "testset-kuro-p"
   :testdesc "kuromoji + phrase-search"
   :dataset dataset-1
   :analyzer t-common/kuromoji-analyzer
   :search-fn core/phrase-search
   :query+result {"20130819" #{"1"}
                  "2013" #{}
                  "hello" #{"2"}
                  "lo" #{}
                  "ton" #{}
                  "東京都庁" #{"13"}
                  "東京" #{"13"}
                  "京都" #{}
                  "都庁" #{"13"}
                  "にち" #{}
                  "バナ ナナ" #{}}})

(def testset-kuro-q
  {:testname "testset-kuro-q"
   :testdesc "kuromoji + qp-search"
   :dataset dataset-1
   :analyzer t-common/kuromoji-analyzer
   :search-fn core/qp-search
   :query+result {"20130819" #{"1"}
                  "2013*" #{"1" "11"}
                  "Hello" #{"2"}
                  "\"Hello\"" #{"2"}
                  "key:12" #{"12"}
                  "東京?庁" #{}
                  "/こん(にち|ばん)は/" #{"12"}
                  "東京~" #{"13"} ; ???
                  "[20130000 TO 20140000]" #{"1" "11"}
                  "Tokyo AND station" #{"3"}
                  "バナ ナナ" #{}}})

(def testset-ngram-s
  {:testname "testset-ngram-s"
   :testdesc "ngram + search"
   :dataset dataset-1
   :analyzer t-common/ngram-analyzer
   :search-fn core/search
   :query+result {"20130819" #{"1" "11"}
                  "2013" #{"1" "11"}
                  "hello" #{"2"}
                  "lo" #{"2"}
                  "ton" #{"3"}
                  "東京都庁" #{"13"}
                  "東京" #{"13"}
                  "京都" #{"13"}
                  "都庁" #{"13"}
                  "にち" #{"12"}
                  "バナ ナナ" #{"14"}}})

(def testset-ngram-p
  {:testname "testset-ngram-p"
   :testdesc "ngram + phrase-search"
   :dataset dataset-1
   :analyzer t-common/ngram-analyzer
   :search-fn core/phrase-search
   :query+result {"20130819" #{"1"}
                  "2013" #{"1" "11"}
                  "hello" #{"2"}
                  "lo" #{"2"}
                  "ton" #{}
                  "東京都庁" #{"13"}
                  "東京" #{"13"}
                  "京都" #{"13"}
                  "都庁" #{"13"}
                  "にち" #{"12"}
                  "バナ ナナ" #{}}})

(def testset-ngram-q
  {:testname "testset-ngram-q"
   :testdesc "ngram + qp-search"
   :dataset dataset-1
   :analyzer t-common/ngram-analyzer
   :search-fn core/qp-search
   :query+result {"20130819" #{"1"}
                  "2013*" #{"1" "11"}
                  "Hello" #{"2"}
                  "\"Hello\"" #{"2"}
                  "key:12" #{"12"}
                  "東京?庁" #{"13"}
                  "/こん(にち|ばん)は/" #{"12"}
                  "東京~" #{"1" "11" "12" "13" "14" "2" "3"} ; ???
                  "[20130000 TO 20140000]" #{"1" "11"}
                  "Tokyo AND station" #{"3"}
                  "バナ ナナ" #{"14"}}})
