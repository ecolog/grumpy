(ns grumpy.core.drafts
  (:refer-clojure :exclude [load])
  (:require
   [clojure.java.io :as io]
   [grumpy.core.files :as files]
   [grumpy.core.jobs :as jobs])
  (:import
   [java.io File]))


(defn create-new [user]
  (jobs/linearize (str "@" user)
    (let [draft-dir (io/file (str "grumpy_data/drafts/@" user))
          draft     (io/file draft-dir "post.edn")
          edn       {:author user :body ""}]
      (.mkdirs draft-dir)
      (spit draft (pr-str edn))
      edn)))


(defn create-edit [post-id]
  (jobs/linearize post-id
    (let [original-dir (io/file (str "grumpy_data/posts/" post-id))
          original     (io/file original-dir "post.edn")
          draft-dir    (io/file (str "grumpy_data/drafts/" post-id))
          draft        (io/file draft-dir "post.edn")]
      (files/copy-dir original-dir draft-dir)
      (files/read-edn-string (slurp draft)))))


(defn load [post-id]
  (jobs/linearize post-id
    (let [draft (io/file (str "grumpy_data/drafts/" post-id) "post.edn")]
      (when (.exists draft)
        (files/read-edn-string (slurp draft))))))


(defn update! [post-id update-fn]
  (jobs/linearize post-id
    (let [draft  (load post-id)
          draft' (update-fn draft)
          dir    (str "grumpy_data/drafts/" post-id)
          file   (io/file dir "post.edn")]
      (spit file (pr-str draft'))
      draft')))


(defn delete! [post-id]
  (jobs/linearize post-id
    (files/delete-dir (str "grumpy_data/drafts/" post-id))))