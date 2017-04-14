(ns clucie.t-fixture)

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
