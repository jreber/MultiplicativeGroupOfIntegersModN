(ns mult-mod.core
  #_(:refer-clojure :exclude (+ - * /))
  (:require [ubergraph.core :as uber]
            [editscript.core :as ediff])
  (:gen-class))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; graph demo code
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def airports
  (-> (uber/multigraph
        ; city attributes
        [:Artemis {:population 3000}]
        [:Balela {:population 2000}]
        [:Coulton {:population 4000}]
        [:Dentana {:population 1000}]
        [:Egglesberg {:population 5000}]
        ; airline routes
        [:Artemis :Balela {:color :blue, :airline :CheapAir, :price 200, :distance 40}]
        [:Artemis :Balela {:color :green, :airline :ThriftyLines, :price 167, :distance 40}]
        [:Artemis :Coulton {:color :green, :airline :ThriftyLines, :price 235, :distance 120}]
        [:Artemis :Dentana {:color :blue, :airline :CheapAir, :price 130, :distance 160}]
        [:Balela :Coulton {:color :green, :airline :ThriftyLines, :price 142, :distance 70}]
        [:Balela :Egglesberg {:color :blue, :airline :CheapAir, :price 350, :distance 50}])
    (uber/add-directed-edges
      [:Dentana :Egglesberg {:color :red, :airline :AirLux, :price 80, :distance 50}]
      [:Egglesberg :Coulton {:color :red, :airline :AirLux, :price 80, :distance 30}]
      [:Coulton :Dentana {:color :red, :airline :AirLux, :price 80, :distance 65}])))


(uber/pprint airports)
(uber/viz-graph airports)


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
;; define graph algebra and operations
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defprotocol Graph
  (nodes [this] "Returns the nodes of the graph")
  (edges [this] "Returns seq of the edges of the graph")
  (traverse [this node edge] "Returns the node from traversing from node via edge"))

(defrecord MapGraph [_nodes]
  Graph
  (nodes [this] (keys _nodes))
  (edges [this] (for [start (nodes this)
                      edge (keys (_nodes start))]
                  [start (traverse this start edge) edge]))
  (traverse [this node edge] (get-in _nodes [node edge])))

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
(defn group->ubergraph [group]
  (let [graph (graph group)]
    (apply uber/add-directed-edges
           (apply uber/multigraph (nodes graph))
           (for [[start end edge] (edges graph)]
             [start end {:label (format "*%s" edge)}]))))

(def visualize-graph
  (comp uber/viz-graph group->ubergraph))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; REPL helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(comment
  ;; visualize graph for a sample group
  (visualize-graph 11)

  ;; what different between two successive groups?
  (ediff/diff (:_nodes (graph 5))
              (:_nodes (graph 6))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; misc
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
