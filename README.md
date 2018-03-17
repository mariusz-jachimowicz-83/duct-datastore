# duct-datastore
Duct module for configuring and run data stores and apply migrations to them

[![CircleCI](https://circleci.com/gh/mariusz-jachimowicz-83/duct-datastore.svg?style=svg)](https://circleci.com/gh/mariusz-jachimowicz-83/duct-datastore)

[![Dependencies Status](https://jarkeeper.com/mariusz-jachimowicz-83/duct-datastore/status.svg)](https://jarkeeper.com/mariusz-jachimowicz-83/duct-datastore)

## Installation

[![Clojars Project](https://img.shields.io/clojars/v/com.mjachimowicz/duct-datastore.svg)](https://clojars.org/com.mjachimowicz/duct-datastore)

## Usage

To add this module to your configuration, add and configure the `:duct.module.datastore/sql` key:

```clojure
{:duct.module.datastore/sql
 {:datastores {:ds/db1 {:database-url "jdbc:sqlite:db_1"}
               :ds/db2 {:database-url "jdbc:sqlite:db_2"}}
  :migrators {:migrator/m1 {}
              :migrator/m2 {}}
  :seeders {:seeder/s1 {}
            :seeder/s2 {}}
  :environments {:production [[:migrator/m1 :ds/db1]
                              [:migrator/m2 :ds/db2]]
                 :development [[:migrator/m1 :ds/db1]
                               [:migrator/m2 :ds/db2]]
                 :testing []}}}
```

`:datastores` contains all urls to sql databases under keys in form `:some-namespace/some-name` (used as ids).  
After launch there will be db pools created with composite keys in form `[:duct.database.sql/hikaricp :some-namespace/some-name]`.  
You can reference particular db specs by those composite keys.

```clojure
{:component/a {:db (ig/ref [:duct.database.sql/hikaricp :ds/p1])}}
```

`:migrators` key requires to specify ids for migrators. After launch there will be migrators available under composite keys in form `[:duct.migrator/ragtime :some-namespace/some-name]`.  
You can configure each migrator by this composite key.

```clojure
{[:duct.migrator/ragtime :migrator/m2]
 {:migrations-table "migrator_m2",
  :database (ig/ref [:duct.database.sql/hikaricp :ds/p2])
  :strategy :rebase,
  :logger (ig/ref :duct/logger)
  :migrations []}}
```
`:seeders` key requires to specify ids for migrators. After launch there will be migrators available under composite keys in form `[:duct.seeder/ragtime :some-namespace/some-name]`.  
You can configure each migrator by this composite key.

```clojure
{[:duct.seeder/ragtime :seeder/m2]
 {:migrations-table "seeder_m2",
  :database (ig/ref [:duct.database.sql/hikaricp :ds/p2])
  :strategy :rebase,
  :logger (ig/ref :duct/logger)
  :migrations []}}
```

`:environments` key requires to specify combination of databasees and migrators for each your environment. Module will launch onlu db pools and migrators specified in particular environment. 

```clojure
{:environments {:production [[:migrator/m1 :ds/p1]
                             [:migrator/m2 :ds/p2]]
                :development [[:migrator/m1 :ds/p1 :seeder/s1]
                              [:migrator/m2 :ds/p2 :seeder/s2]]}}
```

## License

Copyright © 2018 Mariusz Jachimowicz

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
