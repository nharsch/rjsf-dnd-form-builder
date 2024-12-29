(ns frontend.app
  (:require [uix.core :as uix :refer [defui $]]
            [uix.dom]
            [uix.re-frame :as urf]
            [reagent.core :as r]
            ["@rjsf/core" :default Form]
            ["@rjsf/validator-ajv8" :as validator]
            ["@rjsf/utils" :refer [RJSFSchema]]
            ["@monaco-editor/react" :default Editor]
            ["@dnd-kit/core" :refer [DndContext
                                     useDroppable
                                     useDraggable
                                     closestCenter
                                     PointerSensor
                                     useSensor
                                     useSensors]]
            ["@dnd-kit/utilities" :refer [CSS]]
            ["@dnd-kit/sortable" :refer [useSortable
                                         SortableContext
                                         verticalListSortingStrategy
                                         horizontalListSortingStrategy]]
            ))


(def default-state {:canvas-components [{:id "canvas-text-1" :type "text"}
                                        {:id "canvas-text-2" :type "text"}]
                    :active-item nil
                    :over-item nil
                    ;; :jsonSchema { :title "test form" :type "string" }
                    ;; :uiSchema {}
                    :data {}})

(defonce state (r/atom default-state))

(defn transform-to-string [transform]
  (if transform
    ((.. CSS -Translate -toString) transform)
    "translate3d(0px, 0px, 0px"))


;; TODO: move to utils
(defn insert-at
  ([coll item]
   (vec (concat coll [item])))
  ([coll item idx]
   (vec (concat (take idx coll) [item] (drop idx coll)))))
(defn remove-by-id [coll id]
  (vec (remove #(= (:id %) id) coll)))
(defn get-index-by-id [coll id]
  (some (fn [[idx item]]
          (when (= (:id item) id)
            idx))
        (map-indexed vector coll)))

(defui droppable-context [{:keys [id children]}]
  (let [hook-ret (useDroppable #js {:id id})
        set-node-ref (.-setNodeRef hook-ret)]
    ($ :div {:ref set-node-ref}
       children)))

(defui draggable-item [{:keys [id type children]}]
  (let [hook-ret (useDraggable #js {:id id :data #js {:type type}})
        attributes (.-attributes hook-ret)
        listeners (.-listeners hook-ret)
        set-node-ref (.-setNodeRef hook-ret)
        transform (.-transform hook-ret)
        transition (.-transition hook-ret)
        style {:position "relative"
               :transform (transform-to-string transform)
               :transition transition
               :z-index (if (.-isDragging hook-ret) 1000 0)}]
    ($ :div (merge {:ref set-node-ref
                    :style style
                    :class "draggable"}
                      (js->clj listeners)
                      (js->clj attributes))
       children)))


(defui sortable-item [{:keys [id type children]}]
  (let [hook-ret (useSortable #js {:id id :data #js {:type type}})
        attributes (.-attributes hook-ret)
        listeners (.-listeners hook-ret)
        set-node-ref (.-setNodeRef hook-ret)
        transform (.-transform hook-ret)
        transition (.-transition hook-ret)
        style {:position "relative"
               :transform (transform-to-string transform)
               :transition transition
               :z-index (if (.-isDragging hook-ret) 1000 0)}]
    ($ :div (merge {:ref set-node-ref
                       :style style
                       :class "draggable"}
                      (js->clj listeners)
                      (js->clj attributes))
       children)))


;; TODO should just be for canvas
(defn add-component-to-canvas
  ([state component]
   ;; add to end if no drop target
   (add-component-to-canvas state component (count (:canvas-components state))))
  ([state component drop-target-idx]
   ;; check if id exists in canvas components
   (let [insert-idx drop-target-idx]
     (if (some #(= (:id %) (:id component)) (:canvas-components state))
       ;; reorder
       (let [new-components (-> state
                                (:canvas-components)
                                (remove-by-id (:id component))
                                (insert-at component insert-idx))]
         (assoc state :canvas-components new-components))
       ;; insert as if new
       (let [new-components (insert-at (:canvas-components state) component insert-idx)]
         (assoc state :canvas-components new-components))))))

(defn is-over-canvas [over]
  (cond
    ; TODO: a little hacky, we should look at state for ids
    (re-find #"canvas" over) true
    (re-find #"toolbox" over) false
    :else false))


(defn onDragEnd [event]
  ;; (js/console.log "onDragEnd" event)
  (let [id (.. event -active -id)
        over (.. event -over -id)
        type (.. event -active -data -current -type)
        ;; TODO: would be easier if the idx was passed in the data as key
        over-idx (get-index-by-id (:canvas-components @state) over)]
    (js/console.log event)
    (if (is-over-canvas over)
      ;; TODO: create seperate add and move functions
      (swap! state
             add-component-to-canvas {:id (if (re-find #"toolbox-" id)
                                            ;; generate new id for toolbox items
                                            (str "canvas-" type "-" (rand-int 1000))
                                            ;; use existing id for canvas items
                                            id)
                                      :type type
                                      }
             over-idx))
    ;; clear active
    (swap! state
           update-in [:active-item] (fn [_] nil)))
  ;; (println "state" (:canvas-components @state))
  )

; TODO
(defn on-drag-over [event]
  (let [id (.. event -active -id)
        over-id (.. event -over -id)
        type (.. event -active -data -current -type)
        over-idx (get-index-by-id (:canvas-components @state) over-id)]
    (js/console.log "on-drag-over" event)
    (println "over-id" over-id)
    ;; update state active and over
    (swap! state assoc
           :active-item id
           :over-item over-id)
    )
  (println "state" @state)
  )

(defn calculate-current-items [state]
  "Calculate the current items in the canvas based on canvas components and active item location"
  (let [canvas-components (:canvas-components state)
        active-item (:active-item state)
        over-item (:over-item state)]
    (if (and active-item over-item)
      ;; there won't currently be an active-idx
      (let [over-idx (get-index-by-id canvas-components over-item)]
        (cond over-idx
          (let [new-components (-> canvas-components
                                   (insert-at {:id active-item} over-idx))]
            new-components)))
      canvas-components
)))



(defui app []
  (let [state (urf/use-reaction state)
        current-items (calculate-current-items state)]
    (println "current-items" current-items)
    ($ :<>
       ($ :h1 "JSON Schema Form Builder")
       ($ DndContext {:onDragEnd onDragEnd
                      :onDragOver on-drag-over
                      :collisionDetection closestCenter
                      :sensors (useSensors (useSensor PointerSensor))}
          ($ droppable-context {:id "app"}
             ($ :div {:id "toolbox"}
                ($ draggable-item {:id "toolbox-text" :type "text"} "text input")
                ($ draggable-item {:id "toolbox-int" :type "int"} "int input"))
             ($ SortableContext {:strategy verticalListSortingStrategy
                                 :id "canvas"
                                 :items (clj->js (map :id current-items))}
                (map-indexed
                 (fn [idx component]
                   ($ sortable-item
                      {:id (:id  component) :type (:type component) :key (:id component)}
                      (:id component)))
                 (:canvas-components state)))))

       ;; ($ Form {:schema (clj->js (:jsonSchema state)) :validator validator})
       ;; ($ Editor {:defaultLanguage "JSON"
       ;;            :defaultValue (-> state
       ;;                              :jsonSchema
       ;;                              clj->js
       ;;                              js/JSON.stringify)
       ;;            :height "90vh"})

       )))


(defonce root
  (uix.dom/create-root (js/document.getElementById "root")))


(defn ^:dev/after-load start []
  (uix.dom/render-root ($ app) root))

(comment
  (start)
  )
