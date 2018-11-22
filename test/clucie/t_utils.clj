(ns clucie.t-utils
  (:require [midje.sweet :refer :all]
            [clucie.utils :as utils]))

(facts "keyword->str"
  (tabular
    (fact "qualified keyword returns as string with namespace"
      (utils/keyword->str ?x) => ?expected)
    ?x   ?expected
    :x   "x"
    :x/y "x/y"
    ::x  "clucie.t-utils/x"))

(facts "stringify-value"
       (tabular
         (fact "stringify-value returns given value as string"
           (utils/stringify-value ?x) => ?expected)
         ?x   ?expected
         nil  nil
         1    "1"
         1.1  "1.1"
         "x"  "x"
         'x   "x"
         'x/y "x/y"
         :x   "x"
         :x/y "x/y"
         ::x  "clucie.t-utils/x"))
