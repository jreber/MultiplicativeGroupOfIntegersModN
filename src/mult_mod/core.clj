(ns mult-mod.core
  #_(:refer-clojure :exclude (+ - * /))
  (:require [ubergraph.core :as uber]
            [editscript.core :as ediff]
            [dorothy.core :as dorothy])
  (:import [java.io ByteArrayInputStream]
           [javax.imageio ImageIO])
  (:gen-class))


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
(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
