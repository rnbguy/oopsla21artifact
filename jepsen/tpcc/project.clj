(defproject tpcc "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :main tpcc.core
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [jepsen "0.2.4-SNAPSHOT"]]
  :java-source-paths ["oltp_src/main/java"]
  :resource-paths ["resources/mysql-connector-java-8.0.23.jar"]
  :repl-options {:init-ns tpcc.core})
