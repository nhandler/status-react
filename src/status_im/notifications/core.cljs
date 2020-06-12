(ns status-im.notifications.core
  (:require [re-frame.core :as re-frame]
            [taoensso.timbre :as log]
            [status-im.utils.fx :as fx]
            [status-im.native-module.core :as status]
            ["react-native-push-notification" :as rn-pn]
            [quo.platform :as platform]
            [status-im.utils.utils :as utils]
            [status-im.ui.components.react :as react]))

(defn enable-ios-notifications []
  (.configure
   ^js rn-pn
   (clj->js {:onRegister (fn [token-data]
                           ;;TODO register token in status pn node send waku message
                           (let [token (.-token ^js token-data)]
                             (utils/show-popup nil
                                               (str "Token " token)
                                               #())
                             (react/copy-to-clipboard token)
                             (println "TOKEN " token)))})))

(defn disable-ios-notifications []
  ;;TODO remove token from status pn node, send waku message
  (.abandonPermissions ^js rn-pn))

;; FIXME: Repalce with request permission from audio messages PR lib
(re-frame/reg-fx
 ::request-permission
 identity)

(fx/defn request-permission
  {:events [::request-permission]}
  [_]
  {::request-permission true})

(re-frame/reg-fx
 ::local-notification
 (fn [{:keys [title message]}]
   (log/info {:title   title
              :message message})))

(re-frame/reg-fx
 ::enable
 (fn [_]
   (if platform/android?
     (status/enable-notifications)
     (enable-ios-notifications))))

(re-frame/reg-fx
 ::disable
 (fn [_]
   (if platform/android?
     (status/disable-notifications)
     (disable-ios-notifications))))
