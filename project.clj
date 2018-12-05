(defproject clucie "0.4.3-SNAPSHOT"
  :description "Clojure for the Lucene"
  :url "https://github.com/federkasten/clucie"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :min-lein-version "2.5.0"
  :dependencies [[org.apache.lucene/lucene-core "7.5.0"]
                 [org.apache.lucene/lucene-analyzers-common "7.5.0"]
                 [org.apache.lucene/lucene-analyzers-icu "7.5.0"]
                 [org.apache.lucene/lucene-analyzers-kuromoji "7.5.0"]
                 [org.apache.lucene/lucene-analyzers-phonetic "7.5.0"]
                 [org.apache.lucene/lucene-analyzers-smartcn "7.5.0"]
                 [org.apache.lucene/lucene-analyzers-stempel "7.5.0"]
                 [org.apache.lucene/lucene-queryparser "7.5.0"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.9.0"]
                                  [midje "1.9.4"]]
                   :plugins [[lein-cloverage "1.0.13"]
                             [lein-midje "3.2.1"]]
                   :global-vars {*warn-on-reflection* true
                                 *assert* true}}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :1.10 {:dependencies [[org.clojure/clojure "1.10.0-RC3"]]}}
  :signing {:gpg-key "me@tak.sh"})
