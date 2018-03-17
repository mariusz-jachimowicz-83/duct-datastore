(ns duct.module.datastore
  (:require
    [clojure.string  :as string]
    [clojure.java.io :as io]
    [clojure.string  :as str]
    [duct.core       :as duct]
    [duct.core.env   :as env]
    [duct.core.merge :as merge]
    [integrant.core  :as ig]
    [ragtime.clj.core]))

(defn- get-environment [config options]
  (:environment options (:duct.core/environment config :production)))

(defn- get-project-ns [config options]
  (:project-ns options (:duct.core/project-ns config)))

(defn- ->name [key]
  (-> (format "%s/%s" (namespace key) (name key))
      (string/replace "/" "_")
      (string/replace "-" "_")
      (string/replace "." "_")))

(def ^:private env-strategy
  {:production  :raise-error
   :development :rebase})

(defn- database-config [id jdbc-url]
  (let [key [:duct.database.sql/hikaricp id]]
    #_(derive key [:duct.database/sql id])
    {key ^:demote {:jdbc-url jdbc-url
                   :logger   (ig/ref :duct/logger)}}))


(defn- migrator-config [id environment db-id]
  {[:duct.migrator/ragtime id]
   ^:demote {:migrations-table (->name id)
             :database   (ig/ref [:duct.database.sql/hikaricp db-id])
             :strategy   (env-strategy environment)
             :logger     (ig/ref :duct/logger)
             :migrations []}})

(defn- seeder-config [id environment db-id]
  {[:duct.seeder/ragtime id]
   ^:demote {:migrations-table (->name id)
             :database   (ig/ref [:duct.database.sql/hikaricp db-id])
             :strategy   (env-strategy environment)
             :logger     (ig/ref :duct/logger)
             :migrations []}})


(defmethod ig/init-key :duct.module.datastore/sql [_ options]
  {:req #{:duct/logger}
   :fn  (fn [config]
          (let [environment (get-environment config options)
                components (get-in options [:environments environment])
                dbs (->> (get-in options [:environments environment])
                         (map second)
                         distinct
                         (map #(database-config %
                                                (get-in options [:datastores % :database-url]))))
                migs (map (fn [[migrator-id db-id]]
                            (migrator-config migrator-id environment db-id))
                          components)
                seeds (map (fn [[_ db-id seeder-id]]
                             (println db-id)
                             (when seeder-id
                               (seeder-config seeder-id environment db-id)))
                           components)]
            (->> (concat [config] dbs migs seeds)
                 (apply duct/merge-configs))))})

#_(defmethod ig/init-key :duct.module.datastore/cassandra [_ options]
  {:req #{:duct/logger}
   :fn  (fn [config]
          config)})

#_(defmethod ig/init-key :duct.module.datastore/elastic-search [_ options]
  {:req #{:duct/logger}
   :fn  (fn [config]
          config)})


