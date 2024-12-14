(ns frontend.app
  (:require [uix.core :refer [defui $]]
            [uix.dom])
  )


(defui button [{:keys [on-click children]}]
  ($ :button.btn {:on-click on-click}
    children))

(defui app []
  (let [[state set-state!] (uix.core/use-state 0)]
    ($ :<>
      ($ button {:on-click #(set-state! dec)} "-")
      ($ :span state)
      ($ button {:on-click #(set-state! inc)} "+"))))

(defonce root
  (uix.dom/create-root (js/document.getElementById "root")))




(defn init []
  (println  "Hello, world!!!")
  (uix.dom/render-root ($ app) root)
  )


(comment
  (js/alert "Hello, world!!!")
  (+ 1 3 4)
  (concat '(1 2) '(3 4))
  )
