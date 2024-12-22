(ns frontend.app
  (:require [uix.core :as uix :refer [defui $]]
            [uix.dom]
            [uix.re-frame :as urf]
            [reagent.core :as r]
            ["@rjsf/core" :default Form]
            ["@rjsf/validator-ajv8" :as validator]
            ["@rjsf/utils" :refer [RJSFSchema]]
            ["@monaco-editor/react" :default Editor]
            ["@dnd-kit/core" :refer [DndContext useDroppable useDraggable]]
            ))


(def default-state {:canvas-components []
                    ;; :jsonSchema { :title "test form" :type "string" }
                    ;; :uiSchema {}
                    :data {}})

(defonce state (r/atom default-state))

(defn transform-3D-string [transform]
  (if transform
    (str "translate3d(" (.-x transform) "px, " (.-y transform) "px, 0")))


(defui draggable-item [{:keys [id type children]}]
  (let [hook-ret (useDraggable #js {:id id :data #js {:type type}})
        attributes (.-attributes hook-ret)
        listeners (.-listeners hook-ret)
        set-node-ref (.-setNodeRef hook-ret)
        transform (.-transform hook-ret)
        style {:position "relative"
               :transform (transform-3D-string transform)
               :z-index (if (.-isDragging hook-ret) 1000 0)}]
    ($ :button (merge {
                       :ref set-node-ref
                       :style style
                       }
                      (js->clj listeners)
                      (js->clj attributes))
       children)))


(defui droppable [{:keys [id children]}]
  (let [hook-ret (useDroppable #js {:id id })
        is-over (.-isOver hook-ret)
        set-node-ref (.-setNodeRef hook-ret)
        style {:color (if is-over "green" "black")
               :min-height "100px"}]
    ($ :div {:ref set-node-ref
             :style style}
       children)))


;; I wish this was a built in
;; TODO: move to utils
(defn insert-at
  ([coll item]
   (vec (concat coll [item])))
  ([coll item idx]
   (vec (concat (take idx coll) [item] (drop idx coll)))))

(defn remove-by-id [coll id]
  (vec (remove #(= (:id %) id) coll)))


;; TODO should just be for canvas
(defn add-component-to-canvas
  ([state component]
   (add-component-to-canvas state component (count (:canvas-components state))))
  ([state component drop-target-id]
   ;; check if id exists in canvas components
    (if (some #(= (:id %) (:id component)) (:canvas-components state))
      ;; reorder
      (let [new-components (-> state
                               (:canvas-components)
                               (remove-by-id (:id component))
                               (insert-at component drop-target-id))]
        (assoc state :canvas-components new-components))
      ;; insert as if new
      (let [new-components (insert-at (:canvas-components state) component drop-target-id)]
        (assoc state :canvas-components new-components)))))

(comment
  (add-component-to-canvas default-state {:id "text" :type "text"})
  (-> default-state
      (add-component-to-canvas {:id "text" :type "text"} 0)
      (add-component-to-canvas {:id "text" :type "text"} 0))
  (add-component-to-canvas @state {:id "text" :type "text"} 0)
  (swap! state add-component-to-canvas {:id "text" :type "text"} 0)
  (:canvas-components @state))


(defn onDragEnd [event]
  (js/console.log "onDragEnd" event)
  (let [id (.. event -active -id)
        over (.. event -over -id)
        type (.. event -active -data -current -type)]
    (if (= over "droppable")
      (swap! state
             ;; TODO: dedupe ids
             add-component-to-canvas {:id (if (re-find #"toolbox-" id)
                                            (str "canvas-" type "-" (rand-int 1000))
                                            id)
                                      :type type})))
  (println "state" (:canvas-components @state))
  )


(defui app []
  ;; TODO: use atoms for state instead
  (let [state (urf/use-reaction state)]
       ($ :<>
          ($ :h1 "JSON Schema Form Builder")
          ($ DndContext {:onDragEnd onDragEnd}
             ($ draggable-item {:id "toolbox-text" :type "text"} "text input")
             ($ droppable
                {:id "droppable"}
                (map-indexed
                 (fn [idx component]
                   ($ :li {:key idx}
                      ($ draggable-item {:id (:id  component)
                                         :type (:type component) }
                         (:id component))))
                 (:canvas-components state)
                 )
                )
             )
          ;; ($ Form {:schema (clj->js (:jsonSchema state)) :validator validator})
          ;; ($ Editor {:defaultLanguage "JSON"
          ;;            :defaultValue (-> state
          ;;                              :jsonSchema
          ;;                              clj->js
          ;;                              js/JSON.stringify)
          ;;            :height "90vh"})
          ))
  )


(defonce root
  (uix.dom/create-root (js/document.getElementById "root")))


(defn ^:dev/after-load start []
  (uix.dom/render-root ($ app) root)
  )


(comment
  (start)
  )
