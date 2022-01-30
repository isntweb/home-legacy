(ns smess.core
  (:require
   [smess.login :refer [login-view]]
   [smess.chat.core :refer [chat-view]]
   [reagent.core :as reagent :refer [atom]]))

(defonce app-state (atom {:text "Hello world!"
                          :active-panel :login
                          :user "test"}))

(defonce msg-list (atom []))
(defonce users (atom {}))

(defn app-container
  "The entire front-end application with all of the different views."
  []
  (case (:active-panel @app-state)
    :login [login-view app-state msg-list users]
    :chat [chat-view app-state msg-list users]))

(reagent/render-component [app-container]
                          (. js/document (getElementById "app")))
