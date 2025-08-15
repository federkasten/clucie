(ns clucie.t-utils
  (:require [clojure.test :refer :all]
            [clucie.utils :as utils]))

(deftest test-keyword-or-symbol?
  (testing "returns true when given value is keyword or symbol"
    (is (utils/keyword-or-symbol? :x))
    (is (utils/keyword-or-symbol? ::x))
    (is (utils/keyword-or-symbol? :x/y))
    (is (utils/keyword-or-symbol? 'x))
    (is (utils/keyword-or-symbol? 'x/y))
    (is (not (utils/keyword-or-symbol? 1)))
    (is (not (utils/keyword-or-symbol? "1")))))

(deftest test-qualified?
  (testing "returns true when given value is qualified ident"
    (is (not (utils/qualified? :x)))
    (is (utils/qualified? ::x))
    (is (utils/qualified? :x/y))
    (is (not (utils/qualified? 'x)))
    (is (utils/qualified? 'x/y))
    (is (not (utils/qualified? 1)))
    (is (not (utils/qualified? "1")))))

(deftest test-keyword->str
  (testing "qualified keyword returns as string with namespace"
    (is (= "x" (utils/keyword->str :x)))
    (is (= "x/y" (utils/keyword->str :x/y)))
    (is (= "clucie.t-utils/x" (utils/keyword->str ::x)))))

(deftest test-stringify-value
  (testing "stringify-value returns given value as string"
    (is (nil? (utils/stringify-value nil)))
    (is (= "1" (utils/stringify-value 1)))
    (is (= "1.1" (utils/stringify-value 1.1)))
    (is (= "x" (utils/stringify-value "x")))
    (is (= "x" (utils/stringify-value 'x)))
    (is (= "x/y" (utils/stringify-value 'x/y)))
    (is (= "x" (utils/stringify-value :x)))
    (is (= "x/y" (utils/stringify-value :x/y)))
    (is (= "clucie.t-utils/x" (utils/stringify-value ::x)))))
