(ns clojure-refactoring.support.source
  (:use [clojure.contrib.find-namespaces :only [find-namespaces-in-dir]])
  (:use [clojure-refactoring.support core paths parsley]))

(declare ns-cache)

(defn find-and-load [namespace]
  (if (find-ns namespace)
    (find-ns namespace)
    (do (require namespace)
        (find-ns namespace))))

(defn find-ns-in-user-dir []
  (->> (java.io.File. (System/getProperty "user.dir"))
       find-namespaces-in-dir
       (map find-and-load)))

(defn does-ns-refer-to-var? [namespace v]
  (when v
    (= (ns-resolve namespace (.sym v)) v)))

(defn- extract-filename [namespace]
  (slime-find-file
   (str
    (.replaceAll
     (.replaceAll (name namespace) "-" "_")
     "\\."
     "/")
    ".clj")))

(defn force-ns-name [namespace]
  (if (symbol? namespace)
    namespace
    (ns-name namespace)))

(defn filename-from-ns [namespace]
  (extract-filename (force-ns-name namespace)))

(defn last-modified [namespace]
  (when-let [a (filename-from-ns namespace)]
    (.lastModified (java.io.File. a))))

(defonce ns-cache (atom {})) ;; a mapping of namespace-symbols to
;; cache entries

(defonce new-namespace-cache (atom {})) ;; a mapping of namespace
;; names to namespace cache entries

(defrecord NameSpaceCacheEntry [time parsley symbols])
;; Time is the time this cache entry was created, parsley is the
;; result of calling parsley after slurping the fil, symbols is a set
;; of parsley symbols

(defn parse-from-ns [namespace-name])

(defn parsley-symbol? [ast]
  (and (map? ast)
       (= (:tag ast) :atom)
       (symbol?
        (read-string (apply str (:content ast))))))

(defn extract-symbols [parsed]
  (->> (parsley-sub-nodes parsed)
       (filter parsley-symbol?)
       (map (comp first :content))
       (into #{})))

(defn new-ns-entry [namespace]
  (when-let [f (filename-from-ns namespace)]
    (let [slurped (slurp f)
          parsed (parse slurped)]
      (NameSpaceCacheEntry. (.lastModified (java.io.File. f))
                            parsed
                            (extract-symbols parsed)))))

(defn ns-in-time? [namespace-name]
  (if-let [cached (@ns-cache namespace-name)]
   (= (last-modified namespace-name) (:time cached))))

(defn entry-from-ns-cache [namespace-name]
  (if-let [cached (@ns-cache namespace-name)]
    (if (= (last-modified namespace-name) (:time cached))
      cached
      (new-ns-entry namespace-name))
    (new-ns-entry namespace-name)))

(defn parsley-from-cache [namespace-name]
  (:parsley entry-from-ns-cache))

(defn reload [ns]
  (do (swap! ns-cache assoc (force-ns-name ns) (new-ns-entry ns))
      (require ns :reload)
      ns))

(defn require-and-return [ns]
  (do
    (if (ns-in-time? ns)
      (reload (ns-name ns)))
    ns))

(defn reload-all-user-ns []
  (pmap #(require-and-return %)
        (find-ns-in-user-dir)))

(defn all-ns-that-refer-to [v]
  (->> (find-ns-in-user-dir)
       (map require-and-return)
       (filter #(does-ns-refer-to-var? % v))))
