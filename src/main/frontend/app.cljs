(ns frontend.app
  (:require
   [goog.object]
   [uix.core :as uix :refer [defui $]]
   [uix.dom]
   [uix.re-frame :as urf]
   [reagent.core :as r]
   ["@rjsf/core" :default Form]
   ["@rjsf/validator-ajv8" :as validator]
   ["@rjsf/utils" :refer [RJSFSchema]]
   ["@monaco-editor/react" :default Editor]
   ["@dnd-kit/core" :refer [DndContext
                            DragOverlay
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
                    :over-idx nil
                    :over-item nil
                    ;; :jsonSchema { :title "test form" :type "string" }
                    ;; :uiSchema {}
                    :data {}})

(defonce state (r/atom default-state))

(defn active-item-over-canvas [state]
  (and (:active-item state)
       (re-find #"toolbox" (:active-item state))
       (re-find #"canvas" (:over-item state))))

(defn transform-to-string [transform]
  (if transform
    ((.. CSS -Translate -toString) transform)
    "translate3d(0px, 0px, 0px"))


;; TODO: move to utils
(defn insert-at
  ; insert after idx
  ([v item idx]
   (vec (concat (take idx v) [item] (drop idx v)))))

(comment
  (= (insert-at [1 2 3] 4 1) [1 4 2 3])
  (= (insert-at [1 2 3] 4 0) [4 1 2 3])
  )

(defn remove-at [v idx]
  (vec (concat (take idx v) (drop (inc idx) v))))

(comment
  (= (remove-at [1 2 3] 1) [1 3])
  (= (remove-at [1 2 3] 0) [2 3])
  (= (remove-at [1 2 3] 2) [1 2]))

(defn move-item [v from to]
  (let [item (nth v from)]
    (-> v
        (remove-at from)
        (insert-at item to))))


(comment
    (= (move-item [1 2 3] 0 2) [2 3 1])
    (= (move-item [1 2 3] 0 1) [2 1 3])
    (= (move-item [1 2 3] 2 0) [3 1 2])
    (= (move-item [1 2 3] 1 1) [1 2 3])
  )

(defn remove-by-id [coll id]
  (vec (remove #(= (:id %) id) coll)))
(defn get-index-by-id [coll id]
  (some (fn [[idx item]]
          (when (= (:id item) id)
            idx))
        (map-indexed vector coll)))
(comment
    (get-index-by-id [{:id "1"} {:id "2"} {:id "3"}] "2")
    (get-index-by-id [{:id "1"} {:id "2"} {:id "3"}] "4")

  )

(defui droppable-context [{:keys [id children]}]
  (let [hook-ret (useDroppable #js {:id id})
        set-node-ref (.-setNodeRef hook-ret)]
    ($ :div {:ref set-node-ref :id id}
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

;; TODO: refactor
;; - remove updating state
;; - add component to canvas
;; - move component in canvas

(defn move-component-in-canvas
  [components active-idx drop-target-idx]
  (move-item components active-idx drop-target-idx))

(comment
  (= (move-component-in-canvas [{:id "1"} {:id "2"} {:id "3"}] 0 2) [{:id "2"} {:id "3"} {:id "1"}])
  (= (move-component-in-canvas [{:id "1"} {:id "2"} {:id "3"}] 1 1) [{:id "1"} {:id "2"} {:id "3"}])
  )

(defn add-component-to-canvas
  [components new-component drop-target-idx]
    (insert-at components new-component drop-target-idx))

(comment
  (= (add-component-to-canvas [{:id "1"} {:id "2"} {:id "3"}] {:id "4"} 1) [{:id "1"} {:id "4"} {:id "2"} {:id "3"}]))

(defn is-over-canvas [over]
  (cond
    ; TODO: a little hacky, we should look at state for ids
    (re-find #"canvas" over) true
    ;; (re-find #"toolbox" over) false
    :else false))


(defn on-drag-over [event]
  ;; (js/console.log "on-drag-over" event)
  (let [id (.. event -active -id)
        over-item (.. event -over -id)
        over-idx (goog.object/getValueByKeys event "over" "data" "current" "sortable" "index")]
                                        ; only update if toolbox is over canvas
    (swap! state assoc :active-item id)
    (if (and (re-find #"toolbox" id))
      (swap! state assoc
             :over-idx over-idx
             :over-item over-item)))
                                        ; TODO remove
  (println "state" @state)
  )

(defn on-drag-end [event]
  ;; (js/console.log "onDragEnd" event)
  (let [id (.. event -active -id)
        over (.. event -over -id)
        type (.. event -active -data -current -type)
        ;; TODO: would be easier if the idx was passed in the data as key
        over-idx (or (goog.object/getValueByKeys event "over" "data" "current" "sortable" "index")
                     (count (:canvas-components @state)))]
    (js/console.log "drag end event" event)
    (if (is-over-canvas over)
      ;; TODO: create seperate add and move functions
      (cond (re-find #"toolbox" id)
            ;; add new item to canvas
            (let [new-component {:id (str "canvas-" type "-" (rand-int 100)) :type type}
                  new-components (add-component-to-canvas (:canvas-components @state) new-component over-idx)]
              (swap! state assoc :canvas-components new-components))

            (re-find #"canvas" id)
            ;; move item in canvas
            (let [current-idx (get-index-by-id (:canvas-components @state) id)
                  new-components (move-component-in-canvas (:canvas-components @state) current-idx over-idx)]
              (swap! state assoc :canvas-components new-components))))
    ;; clear active
    (swap! state
           update-in [:active-item] (fn [_] nil)))
  ;; (println "state" (:canvas-components @state))
  )


(defn calculate-current-items
  "Calculate the current items in the canvas based on canvas components and active item location"
  [state]
  (let [canvas-components (:canvas-components state)
        active-item (:active-item state)
        over-idx (:over-idx state)
        over-item (:over-item state)
        ]
    (cond
      ; only handle if active item is a toolbox item and over item is a canvas item
      (and active-item (re-find #"toolbox-" active-item)
           over-item (re-find #"canvas" over-item))
      (insert-at canvas-components {:id active-item} (or over-idx (count canvas-components)))
      ;; (and active-item (re-find #"toolbox-" active-item))
      ;; (insert-at canvas-components {:id active-item} 0)
      :else
      canvas-components)
    )
  )

(comment
  (conj [{:id "canvas-text-1"} {:id "canvas-text-2"}] {:id "toolbox-text"})
  (get-index-by-id (:canvas-components @state) "canvas-text-2")
  (insert-at (:canvas-components @state) {:id "test"} 0)
  )


(defui app []
  (let [
        state (urf/use-reaction state)
        current-items (calculate-current-items state)
        ]
    (println "current-items" current-items)
    ($ :<>
       ($ :h1 "JSON Schema Form Builder")
       ($ DndContext {:onDragEnd on-drag-end
                      :onDragOver on-drag-over
                      :collisionDetection closestCenter
                      :sensors (useSensors (useSensor PointerSensor))}
          ($ droppable-context {:id "toolbox"}
             (if (not (active-item-over-canvas state))
               ($ draggable-item {:id "toolbox-text" :type "text"} "text input"))
             ($ draggable-item {:id "toolbox-int" :type "int"} "int input"))
          ($ SortableContext {:strategy verticalListSortingStrategy
                              :id "canvas"
                              :items (clj->js (map :id current-items))}
             ($ droppable-context {:id "canvas"}
                (map-indexed
                 (fn [idx component]
                   ($ sortable-item
                      {:id (:id  component) :type (:type component) :key (:id component)}
                      (:id component)))
                 current-items)
                ))
          ($ DragOverlay {}
             (if (:active-item state)
               ($ :div {:id (:active-item state)} (:id (:active-item state)))
               nil)
             ))

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
