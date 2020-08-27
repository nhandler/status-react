(ns status-im.ui.screens.add-new.new-chat.views
  (:require [re-frame.core :as re-frame]
            [status-im.i18n :as i18n]
            [status-im.multiaccounts.core :as multiaccounts]
            [status-im.ui.components.chat-icon.screen :as chat-icon]
            [status-im.ui.components.colors :as colors]
            [status-im.ui.components.icons.vector-icons :as vector-icons]
            [quo.core :as quo]
            [status-im.utils.gfycat.core :as gfycat]
            [status-im.qr-scanner.core :as qr-scanner]
            [status-im.ui.components.list.views :as list]
            [status-im.ui.components.react :as react]
            [status-im.ui.components.topbar :as topbar]
            [status-im.ui.screens.add-new.new-chat.styles :as styles]
            [status-im.utils.debounce :as debounce]
            [status-im.utils.utils :as utils]
            [reagent.core :as reagent])
  (:require-macros [status-im.utils.views :as views]))

(defn- render-row [row _ _]
  [quo/list-item
   {:title    (multiaccounts/displayed-name row)
    :icon     [chat-icon/contact-icon-contacts-tab
               (multiaccounts/displayed-photo row)]
    :chevron  true
    :on-press #(re-frame/dispatch [:chat.ui/start-chat
                                   (:public-key row)])}])

(defn- icon-wrapper [color icon]
  [react/view
   {:style {:width            32
            :height           32
            :border-radius    25
            :align-items      :center
            :justify-content  :center
            :background-color color}}
   icon])

(defn- input-icon
  [state new-contact? entered-nickname]
  (let [icon (if new-contact? :main-icons/add :main-icons/arrow-right)]
    (case state
      :searching
      [icon-wrapper colors/gray
       [react/activity-indicator {:color colors/white-persist}]]

      :valid
      [react/touchable-highlight
       {:on-press #(debounce/dispatch-and-chill [:contact.ui/contact-code-submitted new-contact? entered-nickname] 3000)}
       [icon-wrapper colors/blue
        [vector-icons/icon icon {:color colors/white-persist}]]]

      [icon-wrapper colors/gray
       [vector-icons/icon icon {:color colors/white-persist}]])))

(defn get-validation-label [value]
  (case value
    :invalid
    (i18n/label :t/user-not-found)
    :yourself
    (i18n/label :t/can-not-add-yourself)))

(views/defview new-chat []
  (views/letsubs [contacts      [:contacts/active]
                  {:keys [state ens-name public-key error]} [:contacts/new-identity]]
    [react/view {:style {:flex 1}}
     [topbar/topbar
      {:title  (i18n/label :t/new-chat)
       :modal? true
       :right-accessories
       [{:icon                :qr
         :accessibility-label :scan-contact-code-button
         :on-press            #(re-frame/dispatch [::qr-scanner/scan-code
                                                   {:title   (i18n/label :t/new-chat)
                                                    :handler :contact/qr-code-scanned}])}]}]
     [react/view {:flex-direction :row
                  :padding        16}
      [react/view {:flex          1
                   :padding-right 16}
       [quo/text-input
        {:on-change-text
         #(do
            (re-frame/dispatch [:set-in [:contacts/new-identity :state] :searching])
            (debounce/debounce-and-dispatch [:new-chat/set-new-identity %] 600))
         :on-submit-editing
         #(when (= state :valid)
            (debounce/dispatch-and-chill [:contact.ui/contact-code-submitted false nil] 3000))
         :placeholder         (i18n/label :t/enter-contact-code)
         :show-cancel         false
         :accessibility-label :enter-contact-code-input
         :auto-capitalize     :none
         :return-key-type     :go}]]
      [react/view {:justify-content :center
                   :align-items     :center}
       [input-icon state false nil]]]
     [react/view {:min-height 30 :justify-content :flex-end}
      [react/text {:style styles/message}
       (cond (= state :error)
             (get-validation-label error)
             (= state :valid)
             (str (if ens-name
                    ens-name
                    (gfycat/generate-gfy public-key))
                  " • "
                  (utils/get-shortened-address public-key))
             :else "")]]
     [list/flat-list {:data                      contacts
                      :key-fn                    :address
                      :render-fn                 render-row
                      :enableEmptySections       true
                      :keyboardShouldPersistTaps :always}]]))

(defn- nickname-input [entered-nickname]
  [quo/text-input
   {:on-change-text      #(reset! entered-nickname %)
    :auto-capitalize     :none
    :auto-focus          false
    :accessibility-label :nickname-input
    :placeholder         (i18n/label :t/add-nickname)
    :return-key-type     :done
    :auto-correct        false}])

(views/defview new-contact []
  (views/letsubs [{:keys [state ens-name public-key error]} [:contacts/new-identity]
                  entered-nickname (reagent/atom "")]
    [react/view {:style {:flex 1}}
     [topbar/topbar
      {:title  (i18n/label :t/new-contact)
       :modal? true
       :right-accessories
       [{:icon                :qr
         :accessibility-label :scan-contact-code-button
         :on-press            #(re-frame/dispatch [::qr-scanner/scan-code
                                                   {:title        (i18n/label :t/new-contact)
                                                    :handler      :contact/qr-code-scanned
                                                    :new-contact? true}])}]}]
     [react/view {:flex-direction :row
                  :padding        16}
      [react/view {:flex          1
                   :padding-right 16}
       [quo/text-input
        {:on-change-text
         #(do
            (re-frame/dispatch [:set-in [:contacts/new-identity :state] :searching])
            (debounce/debounce-and-dispatch [:new-chat/set-new-identity %] 600))
         :on-submit-editing
         #(when (= state :valid)
            (debounce/dispatch-and-chill [:contact.ui/contact-code-submitted true @entered-nickname] 3000))
         :placeholder         (i18n/label :t/enter-contact-code)
         :show-cancel         false
         :accessibility-label :enter-contact-code-input
         :auto-capitalize     :none
         :return-key-type     :go}]]
      [react/view {:justify-content :center
                   :align-items     :center}
       [input-icon state true @entered-nickname]]]
     [react/view {:min-height 30 :justify-content :flex-end}
      [react/text {:style styles/message}
       (cond (= state :error)
             (get-validation-label error)
             (= state :valid)
             (str (when ens-name (str ens-name " • "))
                  (utils/get-shortened-address public-key))
             :else "")]]
     [quo/list-header
      [quo/text {:accessibility-label :chat-settings
                 :color               :inherit}
       (i18n/label :t/chat-settings)]]
     [react/view {:padding 16}

      [nickname-input entered-nickname]
      [react/text {:style {:margin-top 16 :color colors/gray}}
       (i18n/label :t/nickname-description)]]]))