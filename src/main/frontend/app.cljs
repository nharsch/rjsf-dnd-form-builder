(ns frontend.app
  (:require [uix.core :refer [defui $]]
            [uix.dom]
            ["@rjsf/core" :default Form]
            ["@rjsf/validator-ajv8" :as validator]
            ["@rjsf/utils" :refer [RJSFSchema]]
            ))


(defui button [{:keys [on-click children]}]
  ($ :button.btn {:on-click on-click}
    children))

(defui app []
  ($ Form {:schema #js {:title "Test Form" :type "string"}
           :validator validator}))

(defonce root
  (uix.dom/create-root (js/document.getElementById "root")))


(defn start []
  (uix.dom/render-root ($ app) root)
  )

;; (init)

(comment
  (+ 1 3 4)
  )
