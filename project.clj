(defproject com.mjachimowicz/duct-datastore "0.1.0"
  :description "Duct module for configuring and run multiple datastores and apply migrations to them"
  :url "https://github.com/mariusz-jachimowicz-83/duct-datastore"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-beta4"]
                 [duct/core   "0.6.1"]
                 [duct/logger "0.2.1"]
                 [duct/database.sql.hikaricp "0.3.2"]

                 ;; waiting for my PR to be merged
                 ;; https://github.com/duct-framework/migrator.ragtime/pull/5
                 ;; [duct/migrator.ragtime "0.2.1"]
                 [pandect "0.6.1"]
                 [ragtime "0.7.2"]
                 [com.mjachimowicz/ragtime-clj "0.1.0"]

                 [integrant "0.6.1"]
                 [medley    "1.0.0"]]
  :deploy-repositories [["clojars" {:sign-releases false}]]

  ;; lein cloverage --fail-threshold 95
  ;; lein kibit
  ;; lein eastwood
  :profiles {:dev {:dependencies [[fipp "0.6.12"]
                                  [org.xerial/sqlite-jdbc "3.20.1"]
                                  [org.slf4j/slf4j-nop    "1.7.25"]
                                  [org.clojure/java.jdbc  "0.7.3"]]
                   :plugins [[lein-cloverage "1.0.10"]
                             [lein-kibit "0.1.6"]
                             [jonase/eastwood "0.2.5"]]}})
