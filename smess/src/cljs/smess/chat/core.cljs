(ns smess.chat.core
  (:require
   [smess.chat.input :refer [chat-input]]
   [smess.chat.history :refer [chat-history]]
   [reagent.core :as reagent :refer [atom]]
   [smess.chat.sidebar :refer [sidebar]]))

;; the currently selected message to reply to
(defonce reply-to-message (atom nil))

(defn chat-view
  "Displays all of the chat history."
  [app-state msg-list users]
  [:div {:class "chat-container"}
   [:div {:class "header"}
    [:h3 "smess"]]
   [sidebar users app-state]
   [chat-history msg-list app-state reply-to-message]
   [chat-input app-state reply-to-message]])
