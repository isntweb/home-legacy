(ns smess.notifications)

(defn notify
  "Send a notification with the provided message to the current user."
  [msg app-state]
  (if (not= (.-permission (.-Notification js/window)) "granted")
    (.requestPermission (.-Notification js/window)))
  ;; only send the notification if it was not sent by the current user
  (if (not= (:user msg) (:user @app-state))
    (js/Notification.
     (str "Smess: New message from " (:user msg))
     (clj->js {;; :icon "https://google.com" Add icon later once i have one
               :body (:msg msg)}))))

(defn enable-notifications
  "Enable notifications for this browser."
  []
  (if (.-Notification js/window)
    (if (not= (.-permission (.-Notification js/window)) "granted")
      (.requestPermission js/Notification))
    (.warn js/console "This browser may not have notification capabilities.")))
