(ns grumpy.telegram
  (:require
   [clojure.string :as str]
   [clj-http.client :as http]
   [grumpy.base :as base]
   [grumpy.core :as core]
   [grumpy.config :as config]))


(def ^:dynamic token
  (config/get-optional ::token))


(def ^:dynamic channels
  (or (config/get-optional ::channels)
    #{"grumpy_chat" "whining_test"}))


(defn post! [channel url params]
  (let [url'    (str "https://api.telegram.org/bot" token url)
        params' (assoc params :chat_id channel)]
    (try
      (:body
        (http/post url'
          {:form-params  params'
           :content-type :json
           :as           :json-string-keys}))
      (catch Exception e
        (cond
          (re-find #"Bad Request: message is not modified" (:body (ex-data e)))
          (core/log "Telegram request failed:" url' (pr-str params'))

          :else
          (do
            (core/log "Telegram request failed:" url' (pr-str params'))
            (throw e)))))))


(defn post-picture!
  ([post]
    (reduce post-picture! post channels))
  ([post channel]
   (let [key (cond
               (contains? post :picture-original) :picture-original
               (contains? post :picture) :picture
               :else nil)]
     (cond
       config/dev?  post
       (nil? token) post
       (nil? key)   post
       :else
       (let [picture (get post key)
             url     (str (config/get ::config/hostname) "/post/" (:id post) "/" (:url picture))
             resp    (case (core/content-type picture)
                       :content.type/video (post! (str "@" channel) "/sendVideo" {:video url})
                       :content.type/image (post! (str "@" channel) "/sendPhoto" {:photo url}))]
         (update post :reposts base/conjv
           { :type                :telegram/photo
             :telegram/channel    channel
             :telegram/message_id (get-in resp ["result" "message_id"])
             :telegram/photo      (get-in resp ["result" "photo"]) }))))))


(defn format-user [user]
  (if-some [telegram-user (:telegram/user (base/author-by :user user))]
    (str "@" telegram-user)
    (str "@" user)))


(defn post-text!
  ([post]
   (reduce post-text! post channels))
  ([post channel]
   (cond
     (nil? token) post
     (str/blank? (:body post)) post
     :else
     (let [resp (post! (str "@" channel) "/sendMessage"
                  {:text (str (format-user (:author post)) ": " (:body post))
                   ; :parse_mode "Markdown"
                   :disable_web_page_preview "true"})]
       (update post :reposts base/conjv
         {:type                :telegram/text
          :telegram/channel    channel
          :telegram/message_id (get-in resp ["result" "message_id"]) })))))


(defn update-text! [post]
  (when (some? token)
    (doseq [repost (:reposts post)
            :when  (= :telegram/text (:type repost))]
      (post! (str "@" (:telegram/channel repost))
        "/editMessageText"
        { :message_id (:telegram/message_id repost)
          :text       (str (format-user (:author post)) ": " (:body post))
          ; :parse_mode "Markdown"
          :disable_web_page_preview "true" })))
  post)


(comment
  (http/post (str "https://api.telegram.org/bot" token "/getUpdates") {:form-params {} :content-type :json :as :json-string-keys})
)