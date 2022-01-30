(ns smess.chat.reply)

(defn scroll-to-last-reply
  "Scroll to center the element clicked onto the page."
  [id]
  (.scrollIntoView (.getElementById js/document (str "msg-" id))
                   (clj->js {:behavior "smooth" :inline "center" :block "center"})))

(defn message-reply
  ;; A reply to a previous message.
  [msg]
  [:div {:class "message-reply"
         :onClick (fn [] (scroll-to-last-reply (:id msg)))}
   (str "> " (:user msg) ": " (:msg msg))])
