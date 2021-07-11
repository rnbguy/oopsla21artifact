(defn prepareKS!
  "returns the string for cqlsh to make the keysapace based on the number of nodes"
  [dcs bench]
  (let [dropComm (str "drop KEYSPACE IF EXISTS " bench ";")
        dcString (reduce (fn [oldDc newDc] (str oldDc ", 'dc_" newDc "':1")) "" dcs)
        finalComm (str dropComm " create KEYSPACE " bench " WITH replication = {'class': 'NetworkTopologyStrategy'" dcString "};")]
    finalComm
    ))

(defn prepareDB!
  "creating keyspace and tables"
  [node test tables bench]
  (when (= (str node) "n1")
    (info ">>> creating keyspace on: " (dns-resolve node))
    (c/exec* (str "/home/ubuntu/cassandra/bin/cqlsh " node " -e \"" (prepareKS! (:nodes test) bench) "\"" ))
    (info ">>> creating tablese on: " (dns-resolve node))
    (c/exec* (str "/home/ubuntu/cassandra/bin/cqlsh " (dns-resolve node)  " -f /home/ubuntu/resource/ddl.cql" ))
    (info ">>> keyspace and tables intialized")
    (doseq [t tables]
      (do ; .csv solution:
          ;(info (str "Loading raw data in table: " t))
          ;(c/exec* (str "/home/ubuntu/cassandra/bin/cqlsh " (dns-resolve node)  " -f /home/ubuntu/resource/load_" t ".cql" ))

          ; snapshot based solution:
          (info (str "Copying snapshots for: " t))
          (c/exec* (str "/home/ubuntu/cassandra/bin/sstableloader  -d " (dns-resolve node) " /home/ubuntu/" bench "/" t))
          ))
    ;(c/exec* (str "/home/ubuntu/cassandra/bin/cqlsh " (dns-resolve node)  " -f /home/ubuntu/load.cql" ))
    (Thread/sleep 1000)
    (info (str ">>> initial SSTables loaded ")
    (info ">>> creating a java connection pool")
    )))


