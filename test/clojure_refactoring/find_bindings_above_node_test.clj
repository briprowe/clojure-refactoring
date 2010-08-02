(ns clojure-refactoring.find-bindings-above-node-test
  (:use clojure-refactoring.find-bindings-above-node :reload)
  (:use clojure.test))

(deftest extract_destructured_maps
  (is (= (extract-destructured-maps '[{a :a}])
         '[a]))
  (is (= (extract-destructured-maps '[{b :a}])
         '[b])))

(deftest let_bind
  (is (= (find-bindings-above-node '(defn myfn [a] (let [a 1] (+ 1 a)))
                                   '(+ 1 a))
         '[a])))

(deftest find_bindings
  (is (= (find-bindings-above-node
          '(defn myfn [a] (+ 1 a)) '(+ 1 a))
         '[a]))
  (is (= (find-bindings-above-node
          '(let [a 1] a) 'a)
         '[a]))
  (testing "nested binding"
    (is (= (find-bindings-above-node
            '(let [a 1] (let [b 2] (+ a b))) '(+ a b))
           '[a b])))
  (is (= (find-bindings-above-node
          '(do (let [a 1] (+ a 1)) (let [b 1] (+ 1 b)))
          '(+ 1 b))
         '[b]))
  (testing "destructured bindings"
    (is (= (find-bindings-above-node
            '(let [{a :a} {:a 1}] (+ 1 a))
            '(+ 1 a))
           '[a]))
    (is (= (find-bindings-above-node
            '(let [{:keys [a b]} {:a 1 :b 2}] (+ a b))
            '(+ a b))
           '[a b]))))