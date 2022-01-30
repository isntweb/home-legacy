(ns smess.chat.username)

(defn username-box
  "An interactive box containing the username."
  [username app-state]
  [:p {:key username
       :class (str "username" (if (= (:user @app-state) username) " my-username" ""))}
   (if (= (:user @app-state) username)
     (str "me [ " username " ]")
     username)])
