(ns grumpy.main
  (:require
   [com.stuartsierra.component :as component]
   [grumpy.core.log :as log]
   [grumpy.migrations :as migrations]
   [grumpy.server :as server]))


(defn system [opts]
  (component/system-map
    :server (server/server (:server opts))))


(Thread/setDefaultUncaughtExceptionHandler
  (reify Thread$UncaughtExceptionHandler
    (uncaughtException [_ thread ex]
      (log/log "Uncaught exception on" (.getName ^Thread thread))
      (.printStackTrace ^Throwable ex))))


(defn -main [& {:as args}]
  (migrations/migrate!)
  (let [host (get args "--host")
        port (some-> (get args "--port") Integer/parseInt)]
    (-> (system {:server {:host host, :port port}})
        (component/start))))
