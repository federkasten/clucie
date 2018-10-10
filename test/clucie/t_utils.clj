(ns clucie.t-utils
  (:require [midje.sweet :refer :all]
            [clucie.utils :as utils]))

(facts "keyword->str"
  (tabular
    (fact "qualified keyword returns as string with namespace"
      (utils/keyword->str ?x) => ?expected)
    ?x     ?expected
    nil    nil
    123    nil
    "123"  nil
    :123   "123"
    ::123  "clucie.t-utils/123"))
