(ns smess.chat.utils)

(defn to-clipboard
  "Copy a line of text to the clipboard."
  [txt] (.writeText (.-clipboard js/navigator) txt))
