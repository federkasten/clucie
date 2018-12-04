(ns clucie.t-utils
  (:require [midje.sweet :refer :all]
            [clucie.utils :as utils]))

(facts "keyword-or-symbol?"
       (tabular
         (fact "returns true when given value is keyword or symbol"
               (utils/keyword-or-symbol? ?x) => ?expected)
         ?x   ?expected
         :x   truthy
         ::x  truthy
         :x/y truthy
         'x   truthy
         'x/y truthy
         1    falsey
         "1"  falsey))

(facts "qualified?"
       (tabular
         (fact "returns true when given value is qualified ident"
               (utils/qualified? ?x) => ?expected)
         ?x   ?expected
         :x   falsey
         ::x  truthy
         :x/y truthy
         'x   falsey
         'x/y truthy
         1    falsey
         "1"  falsey))

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
