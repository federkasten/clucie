(ns clucie.t-common
  (:require [midje.sweet :refer :all]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [clucie.core :as core]
            [clucie.analysis :as analysis]
            [clucie.store :as store])
  (:import [java.util UUID]
           [java.io File]))

(def test-store (atom nil))
(def doc-analyzer (atom nil))

(def keyword-analyzer (analysis/keyword-analyzer))
(def standard-analyzer (analysis/standard-analyzer))
(def cjk-analyzer (analysis/cjk-analyzer))
(def kuromoji-analyzer (analysis/kuromoji-analyzer))
(def ngram-analyzer (analysis/ngram-analyzer 2 8 []))

(defn add-entry! [k document]
  (core/add! @test-store
             [{:key k
               :doc document}]
             [:key :doc]
             @doc-analyzer))

(defn add-entries! [target-entries]
  (doseq [entry target-entries]
    (apply add-entry! entry)))

(defn update-entry! [k document]
  (core/update! @test-store
                {:key k
                 :doc document}
                [:key :doc]
                :key k
                @doc-analyzer))

(defn delete-entry! [k]
  (core/delete! @test-store
                :key k
                @doc-analyzer))

(defn delete-entries! [target-entries]
  (doseq [k (map first target-entries)]
    (delete-entry! k)))

(defn search-entries [query-string max-num & [page results-per-page]]
  (core/search @test-store
               [{:doc query-string}]
               max-num
               @doc-analyzer
               page
               results-per-page))

(defn phrase-search-entries [query-string max-num & [page results-per-page]]
  (core/phrase-search @test-store
                      [{:doc query-string}]
                      max-num
                      @doc-analyzer
                      page
                      results-per-page))

(defn wildcard-search-entries [query-string max-num & [page results-per-page]]
  (core/wildcard-search @test-store
                        [{:doc query-string}]
                        max-num
                        @doc-analyzer
                        page
                        results-per-page))

(defn get-tmp-dir [& [specific-dir]]
  (loop []
    (let [uuid4 (.toString (UUID/randomUUID))
          tmpdir (System/getProperty "java.io.tmpdir")
          d (if specific-dir
              (str tmpdir File/separator specific-dir File/separator uuid4)
              (str tmpdir File/separator uuid4))]
      (if (.exists (io/file d))
        (do
          (Thread/sleep 100) ;; Wait to put a clock forward
          (recur))
        d))))

(defn delete-dir! [^File dir-or-file]
  (when (.isDirectory dir-or-file)
    (doseq [child (.listFiles dir-or-file)]
      (delete-dir! child)))
  (.delete dir-or-file))

(defn finish! [& [path]]
  (when @doc-analyzer
    (reset! doc-analyzer nil))
  (when @test-store
    (store/close! @test-store)
    (when path
      (delete-dir! (io/file path)))
    (reset! test-store nil)))

(defn prepare! [analyzer & [path & target-entries]]
  (when @test-store
    (finish! path))
  (reset! doc-analyzer (analysis/analyzer-mapping (analysis/keyword-analyzer)
                                                  {:doc analyzer}))
  (let [store (if path
                (store/disk-store path)
                (store/memory-store))]
    (reset! test-store store)
    (add-entries! (apply concat target-entries))))

(defn all-results-has-score? [results]
  (doseq [r results]
    (assert (number? (:score (meta r))))))

(defn results-is-valid? [quantity & [entry-key]]
  (fn [results]
    (all-results-has-score? results)
    (if (or (zero? quantity) (not entry-key))
      (= quantity (count results))
      (and
        (= quantity (count results))
        (boolean (first (filter #(= entry-key (:key %))
                                results)))))))
