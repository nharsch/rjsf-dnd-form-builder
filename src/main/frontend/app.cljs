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


(defui draggable-item [{:keys [id children]}]
  (let [hook-ret (useDraggable #js {:id id})
        attributes (.-attributes hook-ret)
        listeners (.-listeners hook-ret)
        set-node-ref (.-setNodeRef hook-ret)
        transform (.-transform hook-ret)
        style {:position "relative" :transform (transform-3D-string transform)}]
    ($ :button (merge {
                       :ref set-node-ref
                       :style style
                       }
                      (js->clj listeners)
                      (js->clj attributes))
       children)))


(defui droppable [{:keys [id children]}]
  (let [hook-ret (useDroppable #js {:id id})
        is-over (.-isOver hook-ret)
        set-node-ref (.-setNodeRef hook-ret)
        style {:color (if is-over "green" "black")
               :height "100px"}]
    ($ :div {:ref set-node-ref
             :style style}
       children)))


(defn insert-at
  ([coll item]
   (vec (concat coll [item])))
  ([coll item idx]
   (vec (concat (take idx coll) [item] (drop idx coll)))))

(comment
  (insert-at [1 2 3] {:id "text" :type "text"})
  (insert-at [1 2 3] {:id "text" :type "text"} 2)
  (insert-at [1 2 3] {:id "text" :type "text"} 4)
)

(defn add-component-to-canvas
  ([state component]
   (add-component-to-canvas state component 0))
  ([state component drop-target-id]
   ;; insert component into canvas vector at drop-target-id
   (let [new-components (insert-at (:canvas-components state) component drop-target-id)]
     (assoc state :canvas-components new-components))))


(comment
  (add-component-to-canvas default-state {:id "text" :type "text"})
  (-> default-state
      (add-component-to-canvas {:id "text" :type "text"} 0)
      (add-component-to-canvas {:id "text" :type "text"} 0))
  (add-component-to-canvas @state {:id "text" :type "text"} 0)
  (swap! state add-component-to-canvas {:id "text" :type "text"} 0)
  (:canvas-components @state)
  )

(defn onDragEnd [event]
  ;; (js/console.log "onDragEnd" event)
  (js/console.log (.. event -active -id))
  (swap! state add-component-to-canvas {:id (rand-int 10) :type (.. event -active -id)} 0)
  (println "state" (:canvas-components @state))
  )


(defui app []
  ;; TODO: use atoms for state instead
  (let [state (urf/use-reaction state)]
       ($ :<>
          ($ :h1 "JSON Schema Form Builder")
          ($ DndContext {:onDragEnd onDragEnd}
             ($ draggable-item {:id "text"} "text input")
             ($ droppable
                {:id "droppable"}
                (map-indexed
                 (fn [idx component]
                   ($ :li {:id (:id component) :key idx} (:type component)))
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
