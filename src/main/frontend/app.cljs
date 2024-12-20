(ns frontend.app
  (:require [uix.core :as uix :refer [defui $]]
            [uix.dom]
            ["@rjsf/core" :default Form]
            ["@rjsf/validator-ajv8" :as validator]
            ["@rjsf/utils" :refer [RJSFSchema]]
            ["@monaco-editor/react" :default Editor]
            ["@dnd-kit/core" :refer [DndContext useDroppable useDraggable]]
            ))


(def default-state {:jsonSchema { :title "test form" :type "string" }
                    :uiSchema {}
                    :data {}})

(defn transform-3D-string [transform]
  (if transform
    (str "translate3d(" (.-x transform) "px, " (.-y transform) "px, 0")))


(defui draggable-item [{:keys [id props]}]
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
       (str "Draggable " id))))


(defui droppable [props]
  (let [hook-ret (useDroppable #js {:id "droppable"})
        is-over (.-isOver hook-ret)
        set-node-ref (.-setNodeRef hook-ret)
        style (if is-over
                {:color "green"})]
    ($ :div {:ref set-node-ref
             :style style}
       (if is-over
         "Drop here"
         "Drag here"))))

;; ($ draggable-item {:id "droppable" :props {}})

(defui app []
  ;; TODO: use atoms for state instead
  ($ :<>
     ($ :h1 "JSON Schema Form Builder")
     ($ DndContext {:handle-drag-end #(js/console.log "drag end")}
        ($ draggable-item {:id "1" :props {}})
        ($ droppable {})
        )
     ;; ($ Form {:schema (clj->js (:jsonSchema state)) :validator validator})
     ;; ($ Editor {:defaultLanguage "JSON"
     ;;            :defaultValue (-> state
     ;;                              :jsonSchema
     ;;                              clj->js
     ;;                              js/JSON.stringify)
     ;;            :height "90vh"})
     )
  )


(defonce root
  (uix.dom/create-root (js/document.getElementById "root")))


(defn start []
  (uix.dom/render-root ($ app) root)
  )


(comment
  (start)
  )
