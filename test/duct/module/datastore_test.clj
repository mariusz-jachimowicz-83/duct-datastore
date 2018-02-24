(ns duct.module.datastore-test
  (:require
    [clojure.test :refer :all]
    [clojure.string    :as string]
    [clojure.java.io   :as io]
    [clojure.java.jdbc :as jdbc]
    [duct.core         :as duct]
    [duct.logger       :as logger]
    [integrant.core    :as ig]
    [fipp.edn :refer [pprint]]))

(duct/load-hierarchy)

(derive :duct.logger/fake :duct/logger)

(defrecord TestLogger []
  logger/Logger
  (-log [_ level ns-str file line id event data]))

;; fake logger initialization
;; we don't need whole logger subsystem
(defmethod ig/init-key :duct/logger [_ config] (->TestLogger))

(def base-config
  {::duct/environment :development
   :duct/logger {}
   :duct.module.datastore/sql

   {:datastores {:ds/p1 {:database-url "jdbc:sqlite:db_1"}
                 :ds/p2 {:database-url "jdbc:sqlite:db_2"}}
    :migrators {:migrator/m1 {}
                :migrator/m2 {}}
    :environments {:production [[:migrator/m1 :ds/p1]
                                [:migrator/m2 :ds/p2]]
                   :development [[:migrator/m1 :ds/p1]
                                 [:migrator/m2 :ds/p2]]
                   :testing []}}})


(deftest configuration-test
  (is (= (merge base-config
                {[:duct.database.sql/hikaricp :ds/p1]
                 {:jdbc-url "jdbc:sqlite:db_1",
                  :logger (ig/ref :duct/logger)}

                 [:duct.database.sql/hikaricp :ds/p2]
                 {:jdbc-url "jdbc:sqlite:db_2",
                  :logger (ig/ref :duct/logger)}

                 [:duct.migrator/ragtime :migrator/m1]
                 {:migrations-table "migrator_m1",
                  :database (ig/ref [:duct.database.sql/hikaricp :ds/p1]),
                  :strategy :rebase
                  :logger (ig/ref :duct/logger)
                  :migrations []}

                 [:duct.migrator/ragtime :migrator/m2]
                 {:migrations-table "migrator_m2",
                  :database (ig/ref [:duct.database.sql/hikaricp :ds/p2])
                  :strategy :rebase,
                  :logger (ig/ref :duct/logger)
                  :migrations []}})
         (duct/prep base-config))))

(def component-1-config
  {:component/a {:db (ig/ref [:duct.database.sql/hikaricp :ds/p1])}})

(def component-2-config
  {:component/b {:db (ig/ref [:duct.database.sql/hikaricp :ds/p2])}})

(def migrations-config
  {[:duct.migrator/ragtime :migrator/m1]
   {:migrations [(ig/ref ::create-foo)]}

   [:duct.migrator/ragtime :migrator/m2]
   {:migrations [(ig/ref ::create-bar)]}

   [:duct.migrator.ragtime/sql ::create-foo]
   {:up   ["CREATE TABLE foo (id int);"]
    :down ["DROP TABLE foo;"]}

   [:duct.migrator.ragtime/sql ::create-bar]
   {:up   ["CREATE TABLE bar (id int);"]
    :down ["DROP TABLE bar;"]}})

(defmethod ig/init-key :component/a [_ options] options)
(defmethod ig/init-key :component/b [_ options] options)

(defn- find-tables [db-spec]
  (jdbc/query db-spec ["SELECT name FROM sqlite_master WHERE type='table'"]))

(defn spec-4-db? [db-spec db-name]
  (-> (jdbc/query db-spec
                  ["PRAGMA database_list;"]
                  {:row-fn #(-> % :file (string/includes? db-name))})
      set
      (contains? true)))

(defn- table-names [db-spec]
  (set (jdbc/query db-spec
                   ["SELECT name FROM sqlite_master WHERE type='table'"]
                   {:row-fn :name})))

(deftest launch-test
  (testing "launch without errors"
    (let [system (-> (merge migrations-config
                            base-config
                            component-1-config
                            component-2-config)
                     duct/prep
                     ig/init)
          db-spec1 (-> system :component/a :db :spec)
          db-spec2 (-> system :component/b :db :spec)]

      (testing "referencing database"
        (is (spec-4-db? db-spec1 "db_1"))
        (is (spec-4-db? db-spec2 "db_2")))

      (testing "migration table and migrations applied"
        (is (= #{"migrator_m1" "foo"} (table-names db-spec1)))
        (is (= #{"migrator_m2" "bar"} (table-names db-spec2))))

      (try
        (finally
          (ig/halt! system))))))
