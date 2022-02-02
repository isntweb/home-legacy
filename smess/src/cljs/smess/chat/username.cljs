(ns smess.chat.username)

(defn username-box
  "An interactive box containing the username."
  [username app-state]
  (let [is-me (= (:user @app-state) username)]
    [:p {:key username
         :class (str "username" (if is-me " my-username" ""))}
     (if is-me (str "me [ " username " ]") username)]))
