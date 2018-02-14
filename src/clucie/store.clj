(ns clucie.store
  (:require [clojure.java.io :as io])
  (:import [java.nio.file Paths]
           [org.apache.lucene.analysis Analyzer]
           [org.apache.lucene.store NIOFSDirectory RAMDirectory Directory]
           [org.apache.lucene.index IndexWriter IndexWriterConfig IndexReader DirectoryReader]))

(defn memory-store
  "Create a new index in RAM."
  ^org.apache.lucene.store.RAMDirectory
  []
  (RAMDirectory.))

(defn disk-store
  "Create a new index in a directory on disk."
  ^org.apache.lucene.store.NIOFSDirectory
  [dir-path]
  (NIOFSDirectory. (Paths/get (.toURI (io/file dir-path)))))

(defn store-writer
  "Create an IndexWriter."
  ^org.apache.lucene.index.IndexWriter
  [index ^Analyzer analyzer]
  (IndexWriter. index (IndexWriterConfig. analyzer)))

(defn store-reader
  "Create an IndexReader."
  ^org.apache.lucene.index.IndexReader
  [^Directory index]
  (DirectoryReader/open index))

(defn close!
  "Close an index."
  [^Directory index]
  (.close index))

(defn valid-store?
  [^Directory index]
  (DirectoryReader/indexExists index))
