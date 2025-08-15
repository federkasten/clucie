(defproject clucie "0.4.3-SNAPSHOT"
  :description "Clojure for the Lucene"
  :url "https://github.com/federkasten/clucie"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :min-lein-version "2.5.0"
  :dependencies [[org.apache.lucene/lucene-core "9.12.2"]
                 [org.apache.lucene/lucene-analysis-common "9.12.2"]
                 [org.apache.lucene/lucene-analysis-icu "9.12.2"]
                 [org.apache.lucene/lucene-analysis-kuromoji "9.12.2"]
                 [org.apache.lucene/lucene-analysis-phonetic "9.12.2"]
                 [org.apache.lucene/lucene-analysis-smartcn "9.12.2"]
                 [org.apache.lucene/lucene-analysis-stempel "9.12.2"]
                 [org.apache.lucene/lucene-queryparser "9.12.2"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.9.0"]]
                   :plugins [[lein-cloverage "1.0.13"]]
                   :global-vars {*warn-on-reflection* true
                                 *assert* true}}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :1.10 {:dependencies [[org.clojure/clojure "1.10.0-RC3"]]}}
  :signing {:gpg-key "me@tak.sh"})
