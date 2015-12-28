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
  (^CharArraySet [stop-words]
   (char-set stop-words false))
  (^CharArraySet [stop-words ignore-case]
   (CharArraySet. stop-words ignore-case)))

(defn standard-analyzer
  (^org.apache.lucene.analysis.Analyzer []
   (standard-analyzer []))
  (^org.apache.lucene.analysis.Analyzer [stop-words]
   (StandardAnalyzer. (char-set stop-words))))

(defn keyword-analyzer
  ^org.apache.lucene.analysis.Analyzer
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
  (^org.apache.lucene.analysis.Analyzer []
   (CJKAnalyzer.))
  (^org.apache.lucene.analysis.Analyzer [stop-words]
   (CJKAnalyzer. (char-set stop-words))))

(defn- kuromoji-mode [mode]
  (or
    ({:extended JapaneseTokenizer$Mode/EXTENDED
      :normal JapaneseTokenizer$Mode/NORMAL
      :search JapaneseTokenizer$Mode/SEARCH} mode)
    mode
    JapaneseTokenizer$Mode/NORMAL))

(defn kuromoji-analyzer
  (^org.apache.lucene.analysis.Analyzer []
   (JapaneseAnalyzer.))
  (^org.apache.lucene.analysis.Analyzer [user-dict mode stop-words stop-tags]
   (let [mode (kuromoji-mode mode)
         ^CharArraySet stop-words (if (instance? CharArraySet stop-words)
                                    stop-words
                                    (CharArraySet. stop-words false))]
     (JapaneseAnalyzer. user-dict mode stop-words stop-tags))))

;;; TODO: Support to many tokenize options for morphological analyses
(defn- tokenize [^Tokenizer tokenizer ^String text]
  (.setReader tokenizer (StringReader. text))
  (let [^OffsetAttribute offset-attr (.addAttribute tokenizer OffsetAttribute)]
    (.reset tokenizer)
    (loop [results nil]
      (if (.incrementToken tokenizer)
        (let [start (.startOffset offset-attr)
              end (.endOffset offset-attr)]
          (recur (cons (.substring text start end) results)))
        (do
          (.end tokenizer)
          (reverse results))))))

(defn- kuromoji-tokenizer [& [user-dict discard-punctuation? mode factory]]
  (let [discard-punctuation? (boolean discard-punctuation?)
        mode (kuromoji-mode mode)]
    (if factory
      (JapaneseTokenizer. factory user-dict discard-punctuation? mode)
      (JapaneseTokenizer. user-dict discard-punctuation? mode))))

(defn kuromoji-tokenize [text & tokenizer-args]
  (let [^Tokenizer t (apply kuromoji-tokenizer tokenizer-args)
        r (tokenize t text)]
    (.close t)
    r))

(defn analyzer-mapping
  ^org.apache.lucene.analysis.Analyzer
  [default mapping]
  (PerFieldAnalyzerWrapper. default
                            (into {} (map (fn [[k v]] [(name k) v]) mapping))))
