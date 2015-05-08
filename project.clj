(defproject clucie "0.1.0-SNAPSHOT"
  :description "Clojure for the Lucene"
  :url "https://github.com/federkasten/clucie"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.apache.lucene/lucene-core "5.1.0"]
                 [org.apache.lucene/lucene-analyzers-common "5.1.0"]]
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :profiles {:1.5  {:dependencies [[org.clojure/clojure "1.5.0"]]}
             :1.6  {:dependencies [[org.clojure/clojure "1.6.0"]]}})
