(ns smess.chat.markdown
  (:require
   [markdown.core :refer [md->html]]))

(defn markdown-preview
  "A window to preview chat input in markdown."
  [txt]
  [:div {:class "markdown-preview"
         :dangerouslySetInnerHTML {:__html (md->html txt)}}])
