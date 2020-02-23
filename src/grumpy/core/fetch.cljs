(ns grumpy.core.fetch
  (:require
   [grumpy.core.macros :refer [oget oset!]]))


(defn fetch! [method url opts]
  (let [xhr     (js/XMLHttpRequest.)
        success (:success opts)
        error   (:error opts)]
    (.addEventListener xhr "load"
      (fn []
        (this-as resp
          (let [status (oget resp "status")
                body   (oget resp "responseText")]
            (if (not= status 200)
              (do
                (js/console.warn "Error fetching" url ":" body)
                (when (some? error)
                  (error body)))
              (success body))))))
    (when-some [progress (:progress opts)]
      (.addEventListener (oget xhr "upload") "progress"
        (fn [e]
          (when (some? (oget e "lengthComputable"))
            (progress (-> (oget e "loaded") (* 100) (/ (oget e "total")) js/Math.floor))))))
    (.open xhr method url)
    (.send xhr (:body opts))))