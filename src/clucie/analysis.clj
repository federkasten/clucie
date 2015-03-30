(ns clucie.analysis
  (:import [org.apache.lucene.analysis.standard StandardAnalyzer StandardTokenizer StandardFilter]
           [org.apache.lucene.analysis.core KeywordAnalyzer LowerCaseFilter StopFilter]
           [org.apache.lucene.analysis.util CharArraySet]
           [org.apache.lucene.analysis.cjk CJKAnalyzer]
           [org.apache.lucene.analysis.ngram NGramTokenFilter]
           [org.apache.lucene.analysis Analyzer Analyzer$TokenStreamComponents]
           [org.apache.lucene.analysis.miscellaneous PerFieldAnalyzerWrapper]))

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

(defn analyzer-mapping
  ^Analyzer
  [default mapping]
  (PerFieldAnalyzerWrapper. default
                            (into {} (map (fn [[k v]] [(name k) v]) mapping))))
