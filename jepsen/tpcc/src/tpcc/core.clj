(ns tpcc.core
(:require [clojure.tools.logging :refer :all]
          [clojure.string :as str]
          [slingshot.slingshot :refer [try+]]
          [jepsen [cli :as cli]
                  [control :as c]
                  [client :as client]
                  [db :as db]
                  [core :as jepsen]
                  [generator :as gen]
                  [nemesis :as nemesis]
                  [util :as util]
                  [tests :as tests]]
          [tpcc.tpcc-utils :as tpcc]
          [tpcc.sqldump :as sqldump]
          [tpcc.tpcc-asserts :as asserts]
          [jepsen.control.util :as cu]
          [jepsen.os.debian :as debian])
)

(def mysql-dir "/var/lib/mysql")
(def mysql-stock-dir "/var/lib/mysql-stock")

(def cnf-file "/etc/mysql/mariadb.conf.d/50-server.cnf")
(def cnf-stock-file "/etc/mysql/mariadb.conf.d/50-server.cnf.stock")

(def log-files
  ["/var/log/syslog"
   "/var/log/mysql.log"
   "/var/log/mysql.err"
   "/var/lib/mysql/queries.log"])

(defn eval!
  "Evals a mysql string from the command line."
  [s]
  (c/exec :mysql :-u "root" :-e s))

(defn setup-db!
  "Adds a jepsen database to the cluster."
  [node]
  (eval! (str
  "CREATE DATABASE IF NOT EXISTS jepsen;"
  "CREATE USER  'jepsen'@'%';"
  "GRANT ALL PRIVILEGES ON *.* TO 'jepsen'@'%' WITH GRANT OPTION;"
  "FLUSH PRIVILEGES;"))
  )

(defn assert_check
  "Check tpcc assertions"
  []
  (str/join ", " (map
  (fn [i] (+ 1 i))
  (filter
    (fn [i]
      (not= (str/trim (eval! (str "use jepsen;" (nth asserts/assertions i)))) "")
    )
    (range 12)
  )))
)

(defn db
  "galera DB for a particular version."
  [version]
  (reify db/DB
    (setup! [_ test node]
      (c/su
        (when-not (debian/installed? :mariadb-server)
          (c/exec :apt-get :-y :update)
          (c/exec :apt-get :-y :upgrade)
          (debian/install [:mariadb-server])
        )

        (c/exec :service :mysql :stop)
        (when-not (cu/exists? mysql-stock-dir)
          (c/exec :cp :-rp mysql-dir mysql-stock-dir)
        )
        (when-not (cu/exists? cnf-stock-file)
          (c/exec :cp :-p cnf-file cnf-stock-file)
        )

        (c/exec :echo (str "
transaction_isolation=READ-COMMITTED
autocommit=false

[galera]
wsrep_on=ON
wsrep_provider=/usr/lib/galera/libgalera_smm.so
wsrep_cluster_address=gcomm://" (when (not= node (jepsen/primary test)) (jepsen/primary test)) "
binlog_format=row
default_storage_engine=InnoDB
innodb_autoinc_lock_mode=2
innodb_doublewrite=1
query_cache_size=0
bind_address=0.0.0.0
wsrep_cluster_name=\"galera_cluster\"
wsrep_node_address=\"" node "\"
        ") :>> cnf-file)

        (when (= node (jepsen/primary test))
        (c/exec :service :mysql :bootstrap)
        )

        (when (= node (jepsen/primary test)) (setup-db! node) (eval! (str "USE jepsen;" sqldump/sqldump)))

        (jepsen/synchronize test)

        (when (not= node (jepsen/primary test))
        (c/exec :service :mysql :start)
        )

        (jepsen/synchronize test)

        ;; (info node "sleeping")
        ;; (Thread/sleep 30000)
        ;; (info node "woke")
      )
    )

    (teardown! [_ test node]
      (info node "tearing down galera")
      (try
        ;; (when (= node (jepsen/primary test))
        (info node (str "[ASSERT] cr violations: " (assert_check)))
        ;; )
        (catch Exception e (info node (str
          "[RANADEEP] prolly this is beginning of run"
          ;; (.getMessage e)
          )))
      )
      (jepsen/synchronize test)
      (c/exec :service :mysql :stop :|| :echo "prolly not started")
      ;; (jepsen/synchronize test)
      (when (cu/exists? mysql-stock-dir)
        (c/exec :rm :-rf mysql-dir)
        (c/exec :cp :-rp mysql-stock-dir mysql-dir))
      (when (cu/exists? cnf-stock-file)
        (c/exec :rm :-f cnf-file)
        (c/exec :cp :-p cnf-stock-file cnf-file))
      (apply c/exec :truncate :-c :--size 0 log-files)
      )))

(defn no [_ _] {:type :invoke, :f :NO})
(defn pm [_ _] {:type :invoke, :f :PM})
(defn os [_ _] {:type :invoke, :f :OS})
(defn dv [_ _] {:type :invoke, :f :DV})
(defn sl [_ _] {:type :invoke, :f :SL})

(defn parse-long
  "Parses a string to a Long. Passes through `nil`."
  [s]
  (when s (Long/parseLong s)))

(defrecord Client [conn]
  client/Client
  (open! [this test node] (
    assoc this
      :conn
      (java.sql.DriverManager/getConnection (str "jdbc:mysql://" node ":3306/jepsen") "jepsen" "")))


  (setup! [this test]
  ;; (let [stmt (.createStatement (:conn this))] (.executeUpdate stmt "CREATE TABLE IF NOT EXISTS variables") (.close stmt))
  ;; (info node (eval! "SHOW VARIABLES LIKE 'tx_isolation';"))
  ;; (info node (eval! "SHOW VARIABLES LIKE 'autocommit';"))
  (new tpcc.Utils_tpcc 0))

  (invoke! [this test op]
    (info "invoking" op)
    (assoc op :type :fail)
    (try
    (when (= (util/timeout 5000 0
    (do 
    (.setAutoCommit (:conn this) false)
    (case (:f op)
      :NO ((:javaFunc (nth tpcc/operationMap 0)) (:conn this) (tpcc/getNextArgs 1))
      :PM ((:javaFunc (nth tpcc/operationMap 1)) (:conn this) (tpcc/getNextArgs 2))
      :OS ((:javaFunc (nth tpcc/operationMap 2)) (:conn this) (tpcc/getNextArgs 3))
      :DV ((:javaFunc (nth tpcc/operationMap 3)) (:conn this) (tpcc/getNextArgs 4))
      :SL ((:javaFunc (nth tpcc/operationMap 4)) (:conn this) (tpcc/getNextArgs 5))
    )
    (.commit (:conn this))
    (assoc op :type :ok))
    ) 0) (throw (Exception. "transaction timeout")))

    (catch Exception e
      (do
        (info (str "will rollback, caught exception: " (.getMessage e)))
        (.rollback (:conn this))
      )
      )
    )
  )

  (teardown! [this test])

  (close! [this test] (.close (:conn this))))

(defn galera-test
  "Given an options map from the command line runner (e.g. :nodes, :ssh,
  :concurrency, ...), constructs a test map."
  [opts]
  (merge tests/noop-test
         opts
         {:name "galera"
          :os   debian/os
          :db   (db "10.5.9")
          :client (Client. nil)
          :nemesis (nemesis/partitioner (comp nemesis/bridge shuffle))
          :generator (->> (gen/mix [no pm os dv sl])
                    (gen/stagger (float (/ 1 20)))
                    (gen/nemesis (cycle [(gen/sleep 1)
                              {:type :info, :f :start}
                              (gen/sleep 4)
                              {:type :info, :f :stop}]))
                    (gen/time-limit (:time-limit opts)))
          :pure-generators true}))

(defn -main
  "Handles command line arguments. Can either run a test, or a web server for
  browsing results."
  [& args]
  (cli/run! (merge (cli/single-test-cmd {:test-fn galera-test}) (cli/serve-cmd))
            args))
