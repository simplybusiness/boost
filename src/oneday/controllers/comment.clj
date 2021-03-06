(ns oneday.controllers.comment
  (require [oneday.domain :as d]
           [clojure.java.io :as io]
           [jdbc.core :as jdbc]
           [clojure.walk :refer [keywordize-keys]]
           [ring.util.response :as rsp]
           #_ [oneday.views.comment :as v]))

(defn new [req route]
  (let [proposal-id (Integer/parseInt (-> route :route-params :id))
        params (keywordize-keys (:form-params req))
        fields (assoc params
                      :interested (not (empty? (:interested params)))
                      :sponsor (not (empty? (:sponsor params)))
                      :demo (not (empty? (:demo params)))
                      :kudosh (Integer/parseInt (or (:kudosh params) "0"))
                      :author-id (-> req :session :subscriber :id))]
    (if-let [comment (d/add-comment (:db req) proposal-id fields)]
      {:respond (rsp/redirect (str "/proposals/" proposal-id)
                              :see-other)}
      {:respond
       {:status 200
        :headers {"content-type" "text/plain"}
        :body (pr-str fields)}})))
