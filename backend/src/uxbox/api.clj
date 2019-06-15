;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2019 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.api
  (:require [mount.core :refer [defstate]]
            [uxbox.config :as cfg]
            [ring.adapter.jetty :as jetty]
            [reitit.ring :as ring]
            [uxbox.api.middleware :refer [handler router-options]]
            [uxbox.api.auth :as api-auth]
            [uxbox.api.pages :as api-pages]
            [uxbox.api.users :as api-users]
            [uxbox.api.icons :as api-icons]
            [uxbox.api.images :as api-images]
            [uxbox.api.projects :as api-projects]))

;; --- Top Level Handlers

(defn- welcome-api
  "A GET entry point for the api that shows
  a welcome message."
  [context]
  (let [body {:message "Welcome to UXBox api."}]
    {:status 200
     :body {:query-params (:query-params context)
            :form-params (:form-params context)
            :body-params (:body-params context)
            :path-params (:path-params context)
            :params (:params context)}}))

;; --- Routes

(def routes
  (ring/router
   [["/media/*" (ring/create-resource-handler {:root "public/media"})]
    ["/static/*" (ring/create-resource-handler {:root "public/static"})]

    ["/auth/login" {:post (handler #'api-auth/login)}]

    ["/api" {:middleware [api-auth/authorization-middleware]}
     ["/echo" (handler #'welcome-api)]
     ["/projects" {:get (handler #'api-projects/list)
                   :post (handler #'api-projects/create)}]
     ["/projects/by-token/:token" {:get (handler #'api-projects/get-by-share-token)}]
     ["/projects/:id" {:put (handler #'api-projects/update)
                       :delete (handler #'api-projects/delete)}]

     ;; Pages
     ["/pages" {:get (handler #'api-pages/list)
                :post (handler #'api-pages/create)}]
     ["/pages/:id" {:put (handler #'api-pages/update)
                    :delete (handler #'api-pages/delete)}]
     ["/pages/:id/metadata" {:put (handler #'api-pages/update-metadata)}]
     ["/pages/:id/history" {:get (handler #'api-pages/retrieve-history)}]
     ["/pages/:id/history/:hid" {:put (handler #'api-pages/update-history)}]

     ;; Profile
     ["/profile/me" {:get (handler #'api-users/retrieve-profile)
                     :put (handler #'api-users/update-profile)}]
     ["/profile/me/password" {:put (handler #'api-users/update-password)}]
     ["/profile/me/photo" {:post (handler #'api-users/update-photo)}]

     ;; Library
     ["/library"
      ;; Icons
      ["/icon-collections/:id" {:put (handler #'api-icons/update-collection)
                                :delete (handler #'api-icons/delete-collection)}]
      ["/icon-collections" {:get (handler #'api-icons/list-collections)
                             :post (handler #'api-icons/create-collection)}]

      ["/icons/:id/copy" {:put (handler #'api-icons/copy-icon)}]

      ["/icons/:id" {:put (handler #'api-icons/update-icon)
                     :delete (handler #'api-icons/delete-icon)}]
      ["/icons" {:post (handler #'api-icons/create-icon)
                 :get (handler #'api-icons/list-icons)}]

      ;; Images
      ["/image-collections/:id" {:put (handler #'api-images/update-collection)
                                 :delete (handler #'api-images/delete-collection)}]
      ["/image-collections" {:post (handler #'api-images/create-collection)
                             :get (handler #'api-images/list-collections)}]
      ["/images/:id/copy" {:put (handler #'api-images/copy-image)}]
      ["/images/:id" {:get (handler #'api-images/retrieve-image)
                      :delete (handler #'api-images/delete-image)
                      :put (handler #'api-images/update-image)}]
      ["/images" {:post (handler #'api-images/create-image)
                  :get (handler #'api-images/list-images)}]
      ]

     ]]
   router-options))

(def app
  (ring/ring-handler routes (ring/create-default-handler)))

;; --- State Initialization

(defn- start-server
  [config]
  (jetty/run-jetty app {:join? false
                        :async? true
                        :daemon? true
                        :port (:http-server-port config)}))

(defstate server
  :start (start-server cfg/config)
  :stop (.stop server))