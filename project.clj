(defproject clucie "0.1.2"
  :description "Clojure for the Lucene"
  :url "https://github.com/federkasten/clucie"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :min-lein-version "2.5.0"
  :dependencies [[org.apache.lucene/lucene-core "5.5.0"]
                 [org.apache.lucene/lucene-analyzers-common "5.5.0"]
                 [org.apache.lucene/lucene-analyzers-kuromoji "5.5.0"]]
  :plugins [[lein-cloverage "1.0.6"]
            [lein-midje "3.2"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.8.0"]
                                  [midje "1.8.3"]]
                   :global-vars {*warn-on-reflection* true
                                 *assert* true}}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.0"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}}
  :signing {:gpg-key "me@tak.sh"})
