(ns clucie.analysis
  (:import [org.apache.lucene.analysis.standard StandardAnalyzer StandardTokenizer StandardFilter]
           [org.apache.lucene.analysis.core LowerCaseFilter StopFilter]
           [org.apache.lucene.analysis.util CharArraySet]
           [org.apache.lucene.analysis.ngram NGramTokenFilter]
           [org.apache.lucene.analysis Analyzer Analyzer$TokenStreamComponents]))

(defn- char-set
  ^CharArraySet
  ([stop-words]
   (char-set stop-words false))
  ([stop-words ignore-case]
   (CharArraySet. stop-words ignore-case)))

(defn standard-analyzer
  ^Analyzer
  ([]
   (standard-analyzer [] false))
  ([stop-words]
   (StandardAnalyzer. (char-set stop-words))))

(defn ngram-analyzer
  ^Analyzer
  [min-length max-length stop-words]
  (proxy [Analyzer] []
    (createComponents [field-name]
      (let [src (new StandardTokenizer)
            token (-> src
                      (NGramTokenFilter. min-length max-length)
                      (StandardFilter.)
                      (LowerCaseFilter.)
                      (StopFilter. (char-set stop-words)))]
        (proxy [Analyzer$TokenStreamComponents] [src token]
          (setReader [reader]
            (proxy-super setReader reader)))))))
