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
                            ;; DragOverlay
                            useDroppable
                            useDraggable
                            closestCenter
                            PointerSensor
                            useSensor
                            useSensors]]
   ["@dnd-kit/utilities" :refer [CSS]]
   ["@dnd-kit/sortable" :refer [useSortable
                                SortableContext
                                verticalListSortingStrategy]]))

(defui drag-indicator [props]
  ($ :button props
     ($ :svg {:xmlns "http://www.w3.org/2000/svg"
              :width "24"
              :height "24"
              :fill "currentColor"
              :class "mi-outline mi-drag-indicator"
              :viewBox "0 0 24 24"}
        ($ :path {:d "M11 18c0 1.1-.9 2-2 2s-2-.9-2-2 .9-2 2-2 2 .9 2 2m-2-8c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2m0-6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2m6 4c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2m0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2m0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2"}))))



(def toolbox-form-components
  [{:id "stringInput"
    :json-schema {:type "string"}
    :ui-schema {:ui:cardTitle "Text"
               :ui:emptyValue ""
               :ui:widget "text"}}
   {:id "numberInput"
    :json-schema {:type "number"}
    :ui-schema {:ui:cardTitle "Number"
               :ui:widget "updown"}}
   {:id "textareaInput"
    :json-schema {:type "string"}
    :ui-schema {:ui:cardTitle "Text area"
               :ui:widget "textarea"}}
   {:id "boolInput"
    :json-schema {:type "boolean"
                 :oneOf [{:const true :title "Yes"}
                         {:const false :title "No"}]}
    :ui-schema {:ui:cardTitle "Yes/No"
               :ui:widget "radio"}}
   {:id "singleSelectInput"
    :json-schema {:type "array"
                 :uniqueItems true
                 :maxItems 1
                 :items {:type "string"
                         :title "Option"
                         :enum ["option 1" "option 2"]}}
    :ui-schema {:ui:cardTitle "Select"
               :ui:widget "select"}}
   {:id "multiSelectInput"
    :json-schema {:type "array"
                 :uniqueItems true
                 :maxItems 99
                 :items {:type "string"
                         :title "Option"
                         :enum ["option 1" "option 2"]}}
    :ui-schema {:ui:cardTitle "Multi select"
               :ui:widget "select"}}
   {:id "secret"
    :json-schema {:type "string"
                 :minLength 3}
    :ui-schema {:ui:cardTitle "Password"
               :ui:widget "password"}}
   {:id "date"
    :json-schema {:type "string"
                 :format "date"}
    :ui-schema {:ui:cardTitle "Date"}}
   {:id "time"
    :json-schema {:type "string"
                 :format "time"}
    :ui-schema {:ui:cardTitle "Time"}}
   {:id "address"
    :json-schema {:type "object"
                 :required ["street1" "city" "state" "zipCode"]
                 :properties {:street1 {:type "string"}
                              :street2 {:type "string"}
                              :city {:type "string"}
                              :state {:type "string"}
                              :zipCode {:type "number"}}}
    :ui-schema {:ui:cardTitle "Address"}}])


(def toolbox-items [
                    {:id "toolbox-text" :type "text"}
                    {:id "toolbox-int" :type "int"}
                    ])




(defn canvas-components->properties-map [canvas-components]
  (->>
   canvas-components
   (map (fn [component] {(keyword (:id component)) (:json-schema component)}))
   (reduce conj)))

(defn canvas-components->required-vec [canvas-components]
    (->>
     canvas-components
     (filter #(get-in % [:json-schema :required]))
     (map :id)
     (map keyword)
     (vec)))

(defn state->ui-schema [state]
  (let [canvas-components (:canvas-components state)]
    (clj->js
     (merge
      (reduce conj (map (fn [component] {(:id component) (:ui-schema component)}) canvas-components))
      (merge {"ui:order" (map :id canvas-components)})))))

(comment
  (state->ui-schema {:canvas-components [{:id "test" :json-schema {:type "string"} :ui-schema {:ui:cardTitle "test"}}]})
  )



(defn state->json-schema [state]
  (let [canvas-components (:canvas-components state)]
    (clj->js
     {:title (:canvas-title state)
      :type "object"
      :description (:canvas-description state)
      :properties (canvas-components->properties-map canvas-components)
      :required (canvas-components->required-vec canvas-components)})))

(defn active-item-over-canvas [state]
  (and (:active-item state) (:over-canvas state)))

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
  (= (insert-at [1 2 3] 4 0) [4 1 2 3]))

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
    (= (move-item [1 2 3] 1 1) [1 2 3]))

(defn remove-by-id [coll id]
  (vec (remove #(= (:id %) id) coll)))
(defn get-index-by-id [coll id]
  (some (fn [[idx item]]
          (when (= (:id item) id)
            idx))
        (map-indexed vector coll)))
(comment
    (get-index-by-id [{:id "1"} {:id "2"} {:id "3"}] "2")
    (get-index-by-id [{:id "1"} {:id "2"} {:id "3"}] "4"))

(defn id-in-components [id components]
  (some #(= % id) (map :id components)))
(comment
  (= (id-in-components "test" [{:id  "test"} {:id "ing"}]) true)
  (= (id-in-components "test" [{:id  "ing"}]) false))


(defn get-item-by-id [id components]
  (first (filter #(= (:id %) id) components)))
(comment
  (= (get-item-by-id "test" [{:id "test" :data "data"}]) {:id "test" :data "data"}))




(defui droppable-context [{:keys [id style children]}]
  (let [hook-ret (useDroppable #js {:id id})
        set-node-ref (.-setNodeRef hook-ret)]
    ($ :div {:ref set-node-ref :id id :style style}
       children)))

(defui draggable-item [{:keys [id children]}]
  (let [hook-ret (useDraggable #js {:id id })
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




(defn move-component-in-canvas
  [components active-idx drop-target-idx]
  (move-item components active-idx drop-target-idx))
(comment
  (= (move-component-in-canvas [{:id "1"} {:id "2"} {:id "3"}] 0 2) [{:id "2"} {:id "3"} {:id "1"}])
  (= (move-component-in-canvas [{:id "1"} {:id "2"} {:id "3"}] 1 1) [{:id "1"} {:id "2"} {:id "3"}]))

(defn add-component-to-canvas
  [components new-component drop-target-idx]
  (insert-at components new-component drop-target-idx))
(comment
  (= (add-component-to-canvas [{:id "1"} {:id "2"} {:id "3"}] {:id "4"} 1) [{:id "1"} {:id "4"} {:id "2"} {:id "3"}]))

(defn id-in-canvas? [over]
  (cond
                                        ; TODO: a little hacky, we should look at state for ids
    (not over) false
    (re-find #"canvas" over) true
    ;; (re-find #"toolbox" over) false
    :else false))

(defn create-new-component [toolbox-item]
  (assoc toolbox-item :id (str "canvas-" (:id toolbox-item) "-" (js/Date.now))))

(comment
  (create-new-component {:id "test" :json-schema {}})
  (create-new-component (first toolbox-form-components))
  )


(defn calculate-current-items
  "Calculate the current items in the canvas based on canvas components and active item location"
  [state]
  (let [canvas-components (:canvas-components state)
        active-id (:active-item state)
        over-idx (:over-idx state)]
    (cond
      (and (active-item-over-canvas state)                       ; over canvas
           (not (id-in-components active-id canvas-components))) ; not already in components
      (let [active-item (get-item-by-id active-id toolbox-form-components)
            item-name (get-in active-item [:ui-schema :ui:cardTitle])]
        (insert-at canvas-components {:id active-id :name item-name} (or over-idx (count canvas-components))))
      :else
      canvas-components)))

(def default-state {:canvas-title "test title"
                    :canvas-description "test description"
                    :canvas-components []
                    :form-data nil
                    :active-item nil
                    :over-canvas false
                    :data {}})

(defonce state (r/atom default-state))

(defn event-over-canvas [event]
  (let [over-id (.. event -over -id)
        over-container (goog.object/getValueByKeys event "over" "data" "current" "sortable" "containerId")]
    (some id-in-canvas? [over-id over-container])))

(defn on-delete [id]
  (fn [event]
    (swap! state assoc :canvas-components (remove-by-id (:canvas-components @state) id))))

(defn on-id-change [id]
    (fn [event]
        (swap! state assoc :canvas-components (map (fn [component]
                                                    (if (= (:id component) id)
                                                    (assoc component :id (.. event -target -value))
                                                    component))
                                                (:canvas-components @state)))))

(defn on-name-change [id]
    (fn [event]
        (swap! state assoc :canvas-components (map (fn [component]
                                                    (if (= (:id component) id)
                                                      (assoc-in component [:ui-schema :ui:title] (.. event -target -value))
                                                    component))
                                                (:canvas-components @state)))))

(defn on-required-change [id]
  (fn [event]
    (swap! state assoc :canvas-components (map (fn [component]
                                                (if (= (:id component) id)
                                                  (assoc-in component [:json-schema :required] (.. event -target -checked))
                                                component))
                                              (:canvas-components @state)))))

(defui canvas-item [component]
  (let [id (:id component)
        type (get-in component [:json-schema :type])
        name (get-in component [:ui-schema :ui:title])
        hook-ret (useSortable #js {:id id :data #js {:type type}})
        attributes (.-attributes hook-ret)
        listeners (.-listeners hook-ret)
        set-node-ref (.-setNodeRef hook-ret)
        transform (.-transform hook-ret)
        transition (.-transition hook-ret)
        style {:position "relative"
               :transform (transform-to-string transform)
               :transition transition
               :z-index (if (.-isDragging hook-ret) 1000 0)}]
    ($ :div {:ref set-node-ref
             :class "sortable-item"
             :style style
             :key key}
       ($ drag-indicator (merge
                          {:class-name "sortable-handle"}
                          (js->clj listeners)
                          (js->clj attributes)))
       ($ :<>
          ($ :label "type")
          ($ :span type)
          ($ :label "id")
          ($ :input {:class-name "canvas-item-label" :value id :onChange (on-id-change id)})
          ($ :label "name")
          ($ :input {:class-name "canvas-item-label" :value name :onChange (on-name-change id)})
          ($ :label "required")
          ($ :input {:type "checkbox"
                     :name "required"
                     :value (get-in component [:json-schema :required])
                     :checked (get-in component [:json-schema :required])
                     :onChange (on-required-change id)})

          ($ :button {:onClick (on-delete id) :class-name "delete"} "🗑")
          ))))

(defn on-drag-over [event]
  (let [id (.. event -active -id)]
    ;; (js/console.log "onDragOver" event)
    (cond (not= (:active-item @state) id)
          (swap! state assoc :active-item id))
    (let [current-over-canvas (active-item-over-canvas @state)]
      (cond (and (not current-over-canvas)
                 (event-over-canvas event))
            (swap! state assoc :over-canvas true)
            (and current-over-canvas
                 (not (event-over-canvas event)))
            (swap! state assoc :over-canvas false)))))

(defn on-drag-end [event]
  (let [id (.. event -active -id)
        toolbox-item (first (filter #(= (:id %) id) toolbox-form-components))
        over-idx (or (goog.object/getValueByKeys event "over" "data" "current" "sortable" "index")
                     (count (:canvas-components @state)))
        canvas-components (:canvas-components @state)]
    (if (event-over-canvas event)
      (cond (not (id-in-components id canvas-components)) ; new item?
            (let [new-component (create-new-component toolbox-item)
                  new-components (add-component-to-canvas (:canvas-components @state) new-component over-idx)]
              (swap! state assoc :canvas-components new-components))
            (re-find #"canvas" id) ; TODO :else or explicitly check in components
            ;; move item in canvas
            (let [current-idx (get-index-by-id (:canvas-components @state) id)
                  new-components (move-component-in-canvas (:canvas-components @state) current-idx over-idx)]
              (swap! state assoc :canvas-components new-components))))
    ;; clear active
    (swap! state
           update-in [:active-item] (fn [_] nil)
           update-in [:over-canvas] (fn [_] false)
           )))

(defn on-form-change [event]
  (swap! state assoc :form-data (.. event -formData)))

(defn on-title-change [event]
    (swap! state assoc :canvas-title (.. event -target -value)))

(defn on-description-change [event]
    (swap! state assoc :canvas-description (.. event -target -value)))

(add-watch state :debug
  (fn [key atom old-state new-state]
    (println "State changed from" old-state "to" new-state)
    ;; (println "canvas components" (:canvas-components:ponents @state))
    ;; (println "new json schema" (state->json-schema new-state))
    ;; (js/console.log "new ui schema" (state->ui-schema new-state))
    ;; (js/console.log "new form data" (:form-data new-state))
    ))



(defui app []
  (let [state (urf/use-reaction state) ;; state is reframe atom
        current-items (calculate-current-items state)
        rjsf-schema (state->json-schema state)
        ui-schema (state->ui-schema state)]
    ($ :<>
       ($ :section
          ($ :h1 "JSON Schema Form Builder"))
                ($ :input {:type "text"
                           :value (:canvas-title state)
                           :onChange on-title-change})
                ;; TODO: this ought to be a textarea, but rjsf forms don't render line breaks https://github.com/rjsf-team/react-jsonschema-form/issues/2562
                ($ :input {:type "text"
                           :value (:canvas-description state)
                           :onChange on-description-change})
       ($ :section {:id "app"}
          ($ DndContext {:onDragEnd on-drag-end
                         :onDragOver on-drag-over
                         :collisionDetection closestCenter
                         :sensors (useSensors (useSensor PointerSensor))}
             ($ droppable-context {:id "toolbox"}
                ($ :h2 "Toolbox")
                (cond->> toolbox-form-components
                  (active-item-over-canvas state)
                  (remove (fn [item] (= (:id item) (:active-item state))))
                  :always
                  (map (fn [component] ($ draggable-item component (get-in component [:ui-schema :ui:cardTitle]))))))
             ($ SortableContext {:strategy verticalListSortingStrategy
                                 :id "canvas"
                                 :items (clj->js (map :id current-items))}
                ($ droppable-context {:id "canvas"}
                   ($ :h2 "Canvas")
                   (map
                    (fn [component]
                      ($ canvas-item component))
                    current-items))))
          ($ :section {:id "preview"}
             ($ :h2 "Preview")
             ($ Form {:schema rjsf-schema :ui-schema ui-schema :validator validator :on-change on-form-change})
             ($ :h3 "Schema")
             ($ Editor {:height "20rem" :defaultLanguage "json" :value (js/JSON.stringify rjsf-schema nil 2)})
             ($ :h3 "UI Schema")
             ($ Editor {:height "20rem" :defaultLanguage "json" :value (js/JSON.stringify ui-schema nil 2)})
             ($ :h3 "Form Data")
             ($ Editor {:height "20rem" :defaultLanguage "json" :value (js/JSON.stringify (:form-data state) nil 2)} :readOnly true))
          )
       )))

(defonce root
  (uix.dom/create-root (js/document.getElementById "root")))


(defn ^:dev/after-load start []
  (uix.dom/render-root ($ app) root))
