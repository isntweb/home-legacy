(ns smess.handler
  (:require
   [org.httpkit.server :as hk]
   [chord.http-kit :refer [with-channel]]
   [compojure.core :refer :all]
   [compojure.route :as route]
   [clojure.core.async :as async]
   [ring.util.response :as resp]
   [medley.core :refer [random-uuid]]))

; Use a transducer to append a unique id to each message
; creates a channel with buffer of 1 and a list of ids for transducer
; transducer: transformation from one reducing function to another. https://clojure.org/reference/transducers
(defonce main-chan (async/chan 1 (map #(assoc % :id (random-uuid)))))

;; returns multiple of main channel so it can be used repeatedly
(defonce main-mult (async/mult main-chan))

(def users (atom {}))

(defn ws-handler
  [req]
  (with-channel req ws-ch
    ;; get client channel and id
    (let [client-tap (async/chan)
          client-id (random-uuid)]
      (async/tap main-mult client-tap)
      ;; asynchronously executes code in body with loop
      (async/go-loop []
        (async/alt!
          client-tap ([message]
                      (if message
                        (do
                          (async/>! ws-ch message)
                          (recur))
                        (async/close! ws-ch)))
          ws-ch ([{:keys [message]}]
                 (if message
                   (let [{:keys [msg m-type]} message]
                     (if (= m-type :new-user)
                       (do
                         (swap! users assoc client-id msg)
                         (async/>! ws-ch  {:id (random-uuid)
                                           :msg @users
                                           :m-type :init-users})
                         (async/>! main-chan (assoc message :msg {client-id (:msg message)})))
                       (async/>! main-chan message))
                     (recur))
                   (do
                     (async/untap main-mult client-tap)
                     (async/>! main-chan {:m-type :user-left
                                          :msg client-id})
                     (swap! users dissoc client-id)))))))))

(defroutes app
  (GET "/ws" [] ws-handler)
  (GET "/" [] (resp/resource-response "index.html" {:root "public"}))
  (route/resources "/")
  (route/not-found "<h1>Page not found</h1>"))
