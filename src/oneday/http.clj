;; the majority of this file is about routes, not http per se
;; think about renaming it

(ns oneday.http
  (:require [bidi.bidi :as bd]
            [bidi.ring :refer [->Resources]]
            [oneday.page :refer [page]]
            oneday.controllers.proposal
            oneday.controllers.static
            oneday.controllers.comment
            [ring.util.response :as rsp]
            [authomatic.oidc :refer [wrap-oidc]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.content-type :refer (wrap-content-type)]
            [ring.middleware.stacktrace :refer [wrap-stacktrace]]
            [ring.adapter.jetty :refer [run-jetty]]))

(def routes
  ["/" {["static/" [ #"[a-zA-Z0-9/_\.-]+" :path]]
        #'oneday.controllers.static/send-resource
        "proposals/"
        {"" #'oneday.controllers.proposal/index
         "post" #'oneday.controllers.proposal/post
         [:id] #'oneday.controllers.proposal/show
         [:id "/comments/new"] #'oneday.controllers.comment/new
         }}])


(defn app-handler [r]
  (let [route (bd/match-route routes (:uri r))]
    (if route
      (let [controller (:handler route)
            view-data (controller r route)]
        (if-let [view (:view view-data)]
          (rsp/charset (view (dissoc view-data :view)) "UTF-8")
          (:respond view-data)))
      (rsp/content-type (rsp/not-found "not found") "text/plain"))))

;; XXX replace this with real auth (jumpcloud, github, google?)
(defn wrap-auth [h] (fn [r] (h (assoc r :username "daniel-barlow"))))

(defn middlewares [config]
  (-> app-handler
      (wrap-oidc (-> config :oidc :google))
      wrap-params
      wrap-content-type
      wrap-session
      wrap-stacktrace
      ))

(defn start [config]
  (let [db (-> config :db :connection)
        pipeline (middlewares config)
        wrap-db (fn [h] (fn [r] (h (assoc r :db db))))
        server (run-jetty (wrap-db pipeline)
                          (assoc (:http config) :join? false))]
    (assoc-in config [:http :server] server)))

(defn stop [config]
  (.stop (-> config :http :server))
  config)
