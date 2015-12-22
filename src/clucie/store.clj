(ns clucie.store
  (:require [clucie.analysis :refer [standard-analyzer]])
  (:import [java.io StringReader File]
           [java.nio.file Paths]
           [org.apache.lucene.analysis Analyzer]
           [org.apache.lucene.store NIOFSDirectory RAMDirectory Directory]
           [org.apache.lucene.index IndexWriter IndexWriterConfig IndexReader DirectoryReader]
           [org.apache.lucene.util Version]))

(defn memory-store
  "Create a new index in RAM."
  []
  (RAMDirectory.))

(defn disk-store
  "Create a new index in a directory on disk."
  [^String dir-path]
  (NIOFSDirectory. (Paths/get (java.net.URI. (str "file://" dir-path)))))

(defn store-writer
  "Create an IndexWriter."
  ^IndexWriter [index ^Analyzer analyzer]
  (IndexWriter. index (IndexWriterConfig. analyzer)))

(defn store-reader
  "Create an IndexReader."
  ^IndexReader [index]
  (DirectoryReader/open ^Directory index))
