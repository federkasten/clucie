(defproject clucie "0.1.6-SNAPSHOT"
  :description "Clojure for the Lucene"
  :url "https://github.com/federkasten/clucie"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :min-lein-version "2.5.0"
  :dependencies [[org.apache.lucene/lucene-core "6.3.0"]
                 [org.apache.lucene/lucene-analyzers-common "6.3.0"]
                 [org.apache.lucene/lucene-analyzers-kuromoji "6.3.0"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.8.0"]
                                  [midje "1.8.3"]]
                   :plugins [[lein-cloverage "1.0.8"]
                             [lein-midje "3.2.1"]]
                   :global-vars {*warn-on-reflection* true
                                 *assert* true}}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.0"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :1.9 {:dependencies [[org.clojure/clojure "1.9.0-alpha10"]]}}
  :signing {:gpg-key "me@tak.sh"})
