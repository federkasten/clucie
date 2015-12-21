(ns clucie.analysis
  (:import [org.apache.lucene.analysis.standard StandardAnalyzer StandardTokenizer StandardFilter]
           [org.apache.lucene.analysis.core KeywordAnalyzer LowerCaseFilter StopFilter]
           [org.apache.lucene.analysis.util CharArraySet]
           [org.apache.lucene.analysis.cjk CJKAnalyzer]
           [org.apache.lucene.analysis.ngram NGramTokenFilter]
           [org.apache.lucene.analysis.ja JapaneseAnalyzer JapaneseTokenizer JapaneseTokenizer$Mode]
           [org.apache.lucene.analysis Analyzer Analyzer$TokenStreamComponents]
           [org.apache.lucene.analysis.miscellaneous PerFieldAnalyzerWrapper]
           [org.apache.lucene.analysis.tokenattributes OffsetAttribute]
           [org.apache.lucene.analysis Tokenizer]
           [java.io StringReader]))

(defmacro build-analyzer
  [tokenizer & filters]
  `(proxy [org.apache.lucene.analysis.Analyzer] []
     (createComponents [field-name#]
       (let [src# ~tokenizer
             token# (-> src#
                        ~@filters)]
         (proxy [org.apache.lucene.analysis.Analyzer$TokenStreamComponents] [src# token#]
           (setReader [reader#]
             (proxy-super setReader reader#)))))))

(defn- char-set
  ^CharArraySet
  ([stop-words]
   (char-set stop-words false))
  ([stop-words ignore-case]
   (CharArraySet. stop-words ignore-case)))

(defn standard-analyzer
  ^Analyzer
  ([]
   (standard-analyzer []))
  ([stop-words]
   (StandardAnalyzer. (char-set stop-words))))

(defn keyword-analyzer
  ^Analyzer
  []
  (KeywordAnalyzer.))

(defn ngram-analyzer
  [min-length max-length stop-words]
  (build-analyzer (StandardTokenizer.)
                  (NGramTokenFilter. min-length max-length)
                  (StandardFilter.)
                  (LowerCaseFilter.)
                  (StopFilter. (char-set stop-words))))

(defn cjk-analyzer
  ^Analyzer
  ([]
   (CJKAnalyzer.))
  ([stop-words]
   (CJKAnalyzer. (char-set stop-words))))

(defn kuromoji-analyzer
  ^Analyzer
  ([]
   (JapaneseAnalyzer.))
  ([user-dict mode stop-words stop-tags]
   (JapaneseAnalyzer. user-dict mode stop-words stop-tags)))

(defn- kuromoji-mode [mode]
  (or
    ({:extended JapaneseTokenizer$Mode/EXTENDED
      :normal JapaneseTokenizer$Mode/NORMAL
      :search JapaneseTokenizer$Mode/SEARCH} mode)
    mode
    JapaneseTokenizer$Mode/NORMAL))

;;; TODO: Support to many tokenize options
(defn- tokenize [^Tokenizer tokenizer text]
  (.setReader tokenizer (StringReader. text))
  (let [offset-attr (.addAttribute tokenizer OffsetAttribute)]
    (.reset tokenizer)
    (loop [results nil]
      (if (.incrementToken tokenizer)
        (let [start (.startOffset offset-attr)
              end (.endOffset offset-attr)]
          (recur (cons (.substring text start end) results)))
        (do
          (.end tokenizer)
          (reverse results))))))

(defn- kuromoji-tokenizer [& [user-dict discard-puctuation? mode factory]]
  (let [discard-puctuation? (boolean discard-puctuation?)
        mode (kuromoji-mode mode)]
    (if factory
      (JapaneseTokenizer. factory user-dict discard-puctuation? mode)
      (JapaneseTokenizer. user-dict discard-puctuation? mode))))

(defn kuromoji-tokenize [text & tokenizer-args]
  (let [t (apply kuromoji-tokenizer tokenizer-args)]
    (tokenize t text)))

(defn analyzer-mapping
  ^Analyzer
  [default mapping]
  (PerFieldAnalyzerWrapper. default
                            (into {} (map (fn [[k v]] [(name k) v]) mapping))))
