(ns smess.sockets
  (:require
   [smess.notifications :refer [notify]]
   [cljs.core.async :as async :include-macros true]
   [chord.client :refer [ws-ch]]))

(goog-define ws-url "ws://localhost:3449/ws")
(defonce send-chan (async/chan))

;; Websocket Routines
(defn send-msg
  "Send a message over the websocket."
  [msg]
  (async/put! send-chan msg))

(defn send-msgs
  "Send multiple messages over the websocket."
  [svr-chan]
  (async/go-loop []
    (when-let [msg (async/<! send-chan)]
      (async/>! svr-chan msg)
      (recur))))

(defn receive-msgs
  "Receive messages from the websocket."
  [svr-chan app-state msg-list users]
  (async/go-loop []
    (if-let [new-msg (:message (<! svr-chan))]
      (do
        (case (:m-type new-msg)
          :init-users (reset! users (:msg new-msg))
          :chat (do
                  (swap! msg-list conj (dissoc new-msg :m-type))
                  (notify new-msg app-state))
          :new-user (swap! users merge (:msg new-msg))
          :user-left (swap! users dissoc (:msg new-msg)))
        (recur))
      (println "Websocket closed"))))

(defn setup-websockets!
  "Connect websockets to one another."
  [app-state msg-list users]
  (async/go
    (let [{:keys [ws-channel error]} (async/<! (ws-ch ws-url))]
      (if error
        (println (str "Received the websocket error " error))
        (do
          (send-msg {:m-type :new-user
                     :msg (:user @app-state)})
          (send-msgs ws-channel)
          (receive-msgs ws-channel app-state msg-list users))))))
