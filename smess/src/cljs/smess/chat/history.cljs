(ns smess.chat.history
  (:require
   [reagent.core :as reagent]
   [smess.chat.utils :refer [to-clipboard]]
   [smess.chat.username :refer [username-box]]
   [smess.chat.reply :refer [message-reply]]
   [smess.chat.markdown :refer [markdown-preview]]))

(defn- flip-group-chat-results
  "Flip the results of 'group-chats' to display chats top down."
  [msglists]
  (reverse (map (fn [elem] {:user (:user elem)
                            :id (:id elem)
                            :messages (reverse (:messages elem))}) msglists)))

;; input: (list {:msg, :user, :id})
;; return format:
;; list of
;; {:user: current user sending messages
;;  :id: some unique id for the block. can be redundant with messages
;;  :messages: ({:user, :msg, :id})}
(defn- group-chats
  "Reformat chat messages into a better renderable format."
  [message-list]
  (flip-group-chat-results
   (:list (reduce
           (fn [last msg]
             (let
              [last-user (:user last)
               last-list (:list last)]
               (if (= (:user msg) last-user)
            ;; if the current user is the same as the last:
            ;; cons the current message onto the users data structure
                 (let
                  [cur-list-user (:user (first last-list))
                   cur-list-msgs (:messages (first last-list))]
                   {:user cur-list-user
                    :list (cons
                           {:user cur-list-user
                            :messages (cons msg cur-list-msgs)}
                           (rest last-list))})
            ;; otherwise, create a new data structure with the user and the message,
            ;; carrying the last user's information with it
                 (let [ret-obj {:user (:user msg)
                                :list (cons
                                       {:user (:user msg)
                                        :id (:id msg)
                                        :key (:id msg)
                                        :messages (list msg)}
                                       last-list)}]
                   ret-obj))))
            ;; start with an empty user and list
           {:user "" :list '()} message-list))))

(defn focus-element
  "Focus an HTML element with the provided ID."
  [elem-id]
  (let
   [elem (.getElementById js/document elem-id)]
    (.select elem)
    (.focus elem)))

(defn- message
  "A single message."
  [m selected-message]
  (reagent/create-class
   {:render (fn []
              [:div {:key (str "msg-" (:id m))
                     :id (str "msg-" (:id m))
                     :class "message"
                     :style {:background-color (if (= (:id m) (:id @selected-message)) "azure" nil)}}
               (if (:reply-to m) [:div {:class "message-reply-box"} (message-reply (:reply-to m))] nil)
               [:div {:class "message-content"
                      :key (str "message-content" (:id m))}
                (markdown-preview (:msg m))
                [:div {:class "message-buttons"
                       :key (str "message-buttons" (:id m))}
                 [:button {:key (str (:id m) "-text-button")
                           :class "text-button"
                           :onClick (fn [] (to-clipboard (:msg m)))}
                  "copy text"]
                 [:button {:key (str (:id m) "-link-button")} "copy link"]
                 [:button {:key (str (:id m) "-reply-button")
                           :onClick (fn [] (if (= @selected-message m)
                                             (reset! selected-message nil)
                                             (reset! selected-message m))
                                      (focus-element "message-input-box"))}
                  "reply"]]]])}))

(defn chat-history
  "Display the history of the chat."
  [msg-list app-state selected-message]
  (reagent/create-class
   {:render (fn []
              [:div {:class "history"}
               (doall (for [usermsg (group-chats @msg-list)]
                        [:div {:key (str (:user usermsg) "-" (:id usermsg))
                               :class "usermsg"}
                         (username-box (:user usermsg) app-state)
                         (for [m (:messages usermsg)]

                           ^{:key (str "msg-" (:id m))}
                           [message m selected-message])]))])

    :component-did-update (fn [this]
                            (let [node (reagent/dom-node this)]
                              (set! (.-scrollTop node) (.-scrollHeight node))))}))
