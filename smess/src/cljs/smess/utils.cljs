(ns smess.utils)

(defn ormap
  "Returns true if any of the values are true."
  [pred ls] (reduce (fn [acc i] (or acc (pred i))) nil ls))
