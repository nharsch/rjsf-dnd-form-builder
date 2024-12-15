(ns frontend.app
  (:require [uix.core :as uix :refer [defui $]]
            [uix.dom]
            ["@rjsf/core" :default Form]
            ["@rjsf/validator-ajv8" :as validator]
            ["@rjsf/utils" :refer [RJSFSchema]]
            ["@monaco-editor/react" :default Editor]
            ))



(def default-state {:jsonSchema { :title "test form" :type "string" }
                    :uiSchema {}
                    :data {}})


(defui app []
  (let [[state set-state] (uix/use-state default-state)]
    ($ :<>
       ($ Form {:schema (clj->js (:jsonSchema state))
                :validator validator})
       ($ Editor {:defaultLanguage "JSON"
                  :defaultValue (-> state
                                    :jsonSchema
                                    clj->js
                                    js/JSON.stringify)
                  :height "90vh"}))))




(defonce root
  (uix.dom/create-root (js/document.getElementById "root")))


(defn start []
  (uix.dom/render-root ($ app) root)
  )


(comment
  (:jsonSchema default-state)
  (+ 1 3 4)
  )
