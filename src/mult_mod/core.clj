(ns mult-mod.core
  #_(:refer-clojure :exclude (+ - * /))
  (:require [ubergraph.core :as uber]
            [ubergraph.alg :as alg]
            [editscript.core :as ediff]
            [dorothy.core :as dorothy]
            [linked.core :as linked]
            [clojure.pprint :refer [pprint]]
            [juxt.pull.core :refer [pull]])
  (:import [java.io ByteArrayInputStream]
           [javax.imageio ImageIO])
  (:gen-class))


(defn index-of [xs x]
  (first
   (keep-indexed
    #(when (= x %2)
       %1)
    xs)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; code to generate/manipulate multiplication group of numbers modulo n
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn with-mod [f]
  (fn [base]
    (fn [& args]
      (mod (apply f args) base))))

(def +-mod (with-mod +))
(def *-mod (with-mod *))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Define graph algebra and operations. Just generic graph abstraction; not
;; specific to the multiplicative group of integers modulo n.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defprotocol Graph
  (nodes [graph] "Returns the nodes of the graph")
  (edges [graph] "Returns seq of the edges of the graph")
  (filter-edges [graph pred] "Returns a new graph containing only those edges (and destination nodse) which satisfy pred")
  (filter-nodes [graph pred] "Returns a new graph containing only those nodes (and the associated edges) which satisfy pred")
  (traverse [graph node edge] "Returns the node from traversing from node via edge"))

(defrecord MapGraph [_nodes]
  Graph
  (nodes [graph] (keys _nodes))
  (edges [graph] (for [start (nodes graph)
                      edge (keys (_nodes start))]
                  [start (traverse graph start edge) edge]))
  (filter-edges [graph pred] (->MapGraph
                              (->> graph
                                   edges
                                   (filter (fn [[start end edge]]
                                             (pred edge)))
                                   (reduce
                                    (fn [result [start end edge]]
                                      (assoc-in result [start edge] end))
                                    (sorted-map)))))
  (filter-nodes [graph pred] (->MapGraph
                              (->> graph
                                   edges
                                   (filter (fn [[start end edge]]
                                             (and (pred start)
                                                  (pred end))))
                                   (reduce
                                    (fn [result [start end edge]]
                                      (assoc-in result [start edge] end))
                                    (sorted-map)))))
  (traverse [graph node edge] (get-in _nodes [node edge])))

(defn create-graph [edges]
  (->MapGraph
   (reduce
    (fn [result [start end edge]]
      (assoc-in result [start edge] end))
    (sorted-map)
    edges)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; define group algebra and operations
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn group->graph [base]
  (let [xs (range base)]
    (create-graph
     (for [a xs
           b xs]
       [a ((*-mod base) a b) b]))))

(defprotocol MultiplicativeGroup
  (base [this] "Returns the modulo base of the group")
  (graph [this] "Returns the multiplicative graph of the group"))

(extend-type java.lang.Long
  MultiplicativeGroup
  (base [this] this)
  (graph [this] (group->graph this)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; visualizing graphs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn my-graph->ubergraph [graph]
  (apply uber/add-directed-edges
         (apply uber/multigraph (nodes graph))
         (for [[start end edge] (edges graph)]
           [start end {:label (format "×%s" edge)}])))

(def group->ubergraph
  (comp my-graph->ubergraph graph))

(def visualize-graph
  (comp uber/viz-graph group->ubergraph))

(defn ubergraph->png
  "A bit hacky but it got the job done! A hack to change the behavior of
  this line: https://git.io/fjNWO"
  ([ubergraph]
    (ubergraph->png ubergraph nil))
  ([ubergraph opts]
   (with-redefs [dorothy.core/show! #(-> %
                                         (dorothy.core/render {:format :png})
                                         ByteArrayInputStream.
                                         ImageIO/read)]
     (uber/viz-graph ubergraph opts))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; misc REPL helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (let [mod-base 17
        multiplier 7]
    (-> (group->graph mod-base)
        (filter-edges #{multiplier (- mod-base multiplier)})
        (filter-nodes #(not= % 0))
        my-graph->ubergraph
        (uber/viz-graph {:rankdir :LR}))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; misc
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn generate-cycle [m x]
  "Performance: 10s for 7-digit prime."
  (let [next-fn #((*-mod m) x %)]
    (loop [xs (linked/set x)
           prev-x x]
      (let [next-x (next-fn prev-x)]
        (if (xs next-x)
          (conj (vec xs) next-x)
          (recur
           (conj xs next-x)
           next-x))))))

; 849156169
#_(time (count (generate-cycle 1897979 8)))

#_(let [m 49]
  (pprint
   (for [x (range m)]
     (generate-cycle m x))))


(defn midshift-val [m x]
  (let [midpoint (/ m 2)]
    (if (< midpoint x)
      (- x m)
      x)))

(defn midshift-seq [m xs]
  (map (partial midshift-val m) xs))

(defn midshift [m xs]
  (into (linked/set)
        (midshift-seq m xs)))

;; print numbers and counts
#_(let [m 35]
  (pprint
   (reduce
    (fn [result x]
      (let [xs (generate-cycle m x)]
        (assoc result x [(count xs) xs])))
    (sorted-map)
    (range m))))

;; midshifted, with counts
(let [m 36]
  (pprint
   (reduce
    (fn [result x]
      (let [xs (generate-cycle m x)]
        (assoc result (midshift-val m x) [(count (drop-last xs)) (midshift-seq m xs)])))
    (sorted-map)
    (range m))))

(defn cycle-as-traversal [m x]
  (loop [[xx & remaining :as xs] (midshift m (generate-cycle m x))
         new-xs [1]]
    (if (not-empty xs)
      (recur
       remaining
       (conj new-xs
             ((*-mod m) (last new-xs) xx)))
      (midshift-seq m new-xs))))

(defn traverse-cycle [m xs]
  (loop [[xx & remaining :as xs] (midshift m xs)
         new-xs [1]]
    (if (not-empty xs)
      (recur
       remaining
       (conj new-xs
             ((*-mod m) (last new-xs) xx)))
      (midshift-seq m new-xs))))

;; traversing each cycle
#_(let [m 35]
  (pprint
   (reduce
    (fn [result x]
      (let [xs (drop-last (generate-cycle m x))
            traversal (traverse-cycle m xs)]
        (assoc result (midshift-val m x) [(count traversal) traversal])))
    (sorted-map)
    (range m))))

;; partition multiples of [5 7] from non-multiples
;; and union traversed numbers
#_(let [m 35]
  (pprint
   (->> (range m)
        ;; do the traversal
        (reduce
         (fn [result x]
           (let [xs (drop-last (generate-cycle m x))
                 traversal (traverse-cycle m xs)]
             (assoc result (midshift-val m x) [(count traversal) traversal])))
         (sorted-map))
        ;; partition into multiples of [5 7] and not, and union all traversed numbers
        (reduce-kv
         (fn [result x [size traversal]]
           (let [is-factor? #(or
                              (zero? %)
                              (zero? (mod m %)))]
             (update result (is-factor? x) clojure.set/union (set traversal))))
         nil))))

;; partition multiples of [5 7] from non-multiples
;; and union cycle numbers
#_(let [m 35]
  (pprint
   (->> (range m)
        ;; generate cycle
        (reduce
         (fn [result x]
           (let [xs (generate-cycle m x)]
             (assoc result (midshift-val m x) [(count xs) xs])))
         (sorted-map))
        ;; partition into multiples of [5 7] and not, and union all traversed numbers
        (reduce-kv
         (fn [result x [size xs]]
           (let [is-factor? #(or
                              (zero? %)
                              (zero? (mod m %)))]
             (update result (is-factor? x) clojure.set/union xs)))
         nil))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; find mapping between cycles for a particular modulo
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ->subscript-str [n]
  (let [char-diff (- (int \u2080) (int \0))
        ->subscript (fn [c] (-> c
                                int
                                (+ char-diff)
                                char))]
    (->> n
         (Math/abs)
         str
         (map ->subscript)
         (apply str)
         (str (when (neg? n)
                "\u208B")))))

(defn find-mapping [m a b]
  (let [bs (midshift m (generate-cycle m b))
        maybe-match (first
                     (keep-indexed
                      #(when-let [match ((hash-set a (- a)) %2)]
                         [%1 match])
                      bs))]
    (when-let [[i z] maybe-match]
      (format "f%s[n] = %sf%s[%sn]"
              (->subscript-str a)
              (if (= z (- a))
                "(-1)\u207F * " "")
              (->subscript-str b)
              (let [multiplier (midshift-val (count bs) (inc i))]
                (condp = multiplier
                  1 ""
                  -1 "-"
                  multiplier))))))

(find-mapping 35 2 3)

;; print mappings between elements of m
;; partition and print in groups of eight columns because of screen space

(defn find-mappings [m]
  (let [xs (sort (midshift-seq m (range 0 m)))]
    (for [from xs]
      (into {:from from}
            (for [to xs
                  :when (not= from to)]
              (when-let [mapping (find-mapping m from to)]
                [to mapping]))))))

(pprint (find-mappings 36))

(let [m 36]
  (-> (for [{:keys [from] :as mapping} (find-mappings m)
            :when (not= from 1)
            :let [m (dissoc mapping :from)]
            [to fn-str] m]
        [from to fn-str])
      clojure.pprint/pprint
      #_create-graph
      #_my-graph->ubergraph
      #_(uber/viz-graph {:save {:filename "./resources/graph.png"
                              :format :png}})))


(let [m 36
      xs (sort (midshift-seq m (range 1 m)))
      cols xs
      mappings (find-mappings m)]
  (doseq [some-cols [cols]] ;; only works if line wrapping disabled; otherwise, (partition 8 cols)
    (clojure.pprint/print-table
     (cons :from some-cols)
     mappings)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; find if the highest-cardinality cycle is always the most primitive cycle
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(let [m 36]
  (-> (for [{:keys [from] :as mapping} (find-mappings m)
            :when (not= from 1)
            :let [m (dissoc mapping :from)]
            [to fn-str] m]
        [to from fn-str])
      create-graph
      my-graph->ubergraph
      (alg/shortest-path {:start-node 2})))

(defn mappings->ubergraph [m]
  (-> (for [{:keys [from] :as mapping} (find-mappings m)
            :when (not= from 1)
            :let [m (dissoc mapping :from)]
            [to fn-str] m]
        [from to fn-str])
      create-graph
      my-graph->ubergraph)) ;; todo make ubergraph more directly, and 

(defn mappings->ubergraph [m]
  (let [xs (sort (midshift-seq m (range 0 m)))]
    (apply uber/add-directed-edges
           (apply uber/multigraph xs)
           (for [{:keys [from] :as mapping} (find-mappings m)
                 :when (not= from 1)
                 :let [m (dissoc mapping :from)]
                 [to fn-str] m]
             [from to {:label fn-str}])))) ;; TODO turn symmetric relations into undirected edges

(-> 36
    mappings->ubergraph
    (uber/viz-graph {:save {:filename "./resources/graph.png"
                            :format :png}}))

(let [m 36]
  (-> m
      mappings->ubergraph
      (alg/shortest-path {:start-node 8
                          :traverse true})
      alg/paths->graph
      uber/pprint))



(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
