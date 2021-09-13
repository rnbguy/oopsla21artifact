use super::dis_kv::KVStore as DisKVStore;
use super::kv::{KVStoreT, SessionId, VarId};
use super::ser_kv::KVStore as SerKVStore;

use regex::Regex;

use serde::{Deserialize, Serialize};

use serde::de::DeserializeOwned;
use std::fmt::Debug;

use sqlparser::ast;
use sqlparser::ast::Statement::{
    Commit, CreateIndex, CreateTable, Delete, Drop, Insert, Query, Rollback, SetVariable,
    StartTransaction, Update,
};
use sqlparser::dialect;
use sqlparser::parser::Parser;

use msql_srv::QueryResultWriter;

use std::collections::{HashMap, HashSet};

use std::collections::hash_map::DefaultHasher;
use std::hash::{Hash, Hasher};

#[derive(Default)]
pub struct SQLdb {
    ser_kv: SerKVStore,
    dis_kv: DisKVStore,
    loading_done: bool,
    current_session_id: Option<SessionId>,
    n_sql: u64,
}

#[derive(Debug, Hash, Eq, PartialEq, Serialize, Deserialize)]
pub enum KeyTag {
    Columns,
    ColumnKey,
    PrimaryKey,
    LastPrimaryKey,
}

#[derive(Debug, Hash, Eq, PartialEq, Serialize, Deserialize)]
pub enum DataType {
    Int,
    Real,
    Float(Option<u64>),
    Double,
    Boolean,
    Timestamp,
    Varchar(Option<u64>),
    Char(Option<u64>),
    Decimal(Option<u64>, Option<u64>),
}

fn selection_vars(exp: &ast::Expr, vars: &mut HashSet<String>) {
    match exp {
        ast::Expr::Identifier(col) => {
            vars.insert(col.value.clone());
        }
        ast::Expr::BinaryOp { left, right, .. } => {
            selection_vars(left, vars);
            selection_vars(right, vars);
        }
        ast::Expr::Value(_) => {}
        ast::Expr::UnaryOp { op: _, expr: exp } => selection_vars(exp, vars),
        _ => unimplemented!("{:?}", exp),
    }
}

fn selection_eval(exp: &ast::Expr, vals: &HashMap<String, String>) -> String {
    match exp {
        ast::Expr::Identifier(col) => vals.get(&col.value).unwrap().clone(),
        ast::Expr::BinaryOp { left, right, op } => {
            let left_value = selection_eval(left, vals);
            let right_value = selection_eval(right, vals);
            match op {
                ast::BinaryOperator::Plus => format!(
                    "{}",
                    left_value.parse::<u64>().unwrap() + right_value.parse::<u64>().unwrap()
                ),
                ast::BinaryOperator::Eq => format!("{}", left_value == right_value),
                ast::BinaryOperator::NotEq => format!("{}", left_value != right_value),
                ast::BinaryOperator::And => format!(
                    "{}",
                    left_value.parse::<bool>().unwrap() && right_value.parse::<bool>().unwrap()
                ),
                ast::BinaryOperator::Or => format!(
                    "{}",
                    left_value.parse::<bool>().unwrap() || right_value.parse::<bool>().unwrap()
                ),
                ast::BinaryOperator::Lt => format!(
                    "{}",
                    left_value.parse::<f64>().unwrap() < right_value.parse::<f64>().unwrap()
                ),
                ast::BinaryOperator::LtEq => format!(
                    "{}",
                    left_value.parse::<f64>().unwrap() <= right_value.parse::<f64>().unwrap()
                ),
                ast::BinaryOperator::Gt => format!(
                    "{}",
                    left_value.parse::<f64>().unwrap() > right_value.parse::<f64>().unwrap()
                ),
                ast::BinaryOperator::GtEq => format!(
                    "{}",
                    left_value.parse::<f64>().unwrap() >= right_value.parse::<f64>().unwrap()
                ),
                exp => unimplemented!("{:?}", exp),
            }
        }
        ast::Expr::Value(ast::Value::Number(n)) => n.clone(),
        ast::Expr::Value(ast::Value::SingleQuotedString(n)) => n.clone(),
        ast::Expr::UnaryOp { op, expr: k } if op == &ast::UnaryOperator::Minus => {
            format!("-{}", selection_eval(k, vals))
        }
        _ => unimplemented!("{:?}", exp),
    }
}

impl SQLdb {
    pub fn is_ongoing_session(&self, s_id: SessionId) -> bool {
        match self.current_session_id {
            Some(s_id_) if s_id == s_id_ => false,
            None => false,
            _ => true,
        }
    }

    pub fn no_ongoing_session(&self) -> bool {
        self.current_session_id.is_none()
    }

    pub fn set_ongoing_session(&mut self, s_id: &SessionId) {
        // assert!(self.current_session_id.is_none());
        if let Some(ref mut s_id_) = self.current_session_id {
            if *s_id_ != *s_id {
                *s_id_ = *s_id;
            }
        } else {
            self.current_session_id = Some(*s_id);
        }
    }

    pub fn remove_ongoing_session(&mut self) {
        // assert!(self.current_session_id.is_some());
        if self.current_session_id.is_none() {
            // println!("nothing happened before this commit call");
        }
        self.current_session_id = None;
    }

    pub fn execute<W: std::io::Write>(
        &mut self,
        session_id: &SessionId,
        sql: &str,
        results: QueryResultWriter<W>,
    ) -> std::io::Result<()> {
        if sql.starts_with("SET")
            | sql.starts_with("ALTER TABLE")
            | sql.starts_with("CREATE DATABASE")
            | sql.starts_with("SHOW WARNINGS")
        {
            println!("inside SET/ALTER TABLE branch");
            let parsed_sql = Parser::parse_sql(&dialect::AnsiDialect {}, sql)
                .or_else(|_| Parser::parse_sql(&dialect::GenericDialect {}, sql))
                .or_else(|_| Parser::parse_sql(&dialect::MsSqlDialect {}, sql))
                .or_else(|_| Parser::parse_sql(&dialect::MySqlDialect {}, sql))
                .or_else(|_| Parser::parse_sql(&dialect::PostgreSqlDialect {}, sql));

            println!("{:?}", parsed_sql);
            return results.completed(1, 0);
        }

        let sql_lc = sql.to_lowercase();

        if sql_lc == "loading reset" {
            self.loading_done = true;
            println!("LOADING RESET/DONE");

            println!("{:?}", self.dis_kv);

            self.dis_kv.history.drain();

            {
                for (s, vs) in self.ser_kv.setmap.iter() {
                    for (x, &b) in vs.iter() {
                        if b {
                            self.dis_kv.insert(s, x, &0);
                        // println!("DIS KV INSERT {:?} {:?}", s, x);
                        } else {
                            self.dis_kv.delete(s, x, &0);
                            // println!("DIS KV DELETE {:?} {:?}", s, x);
                        }
                    }
                }

                for (x, v) in self.ser_kv.map.iter() {
                    self.dis_kv.write_ser(x, v, &0);
                    // println!("DIS KV WRITE {:?} {:?}", x, v);
                }

                self.dis_kv.commit(&0);
            }
            return results.completed(1, 0);
        } else if sql_lc == "check consistency" {
            let level = self.dis_kv.minimal_consistency();

            let mut col_writer = Vec::new();

            let col = msql_srv::Column {
                table: "Database".into(),
                column: "Consistency".into(),
                coltype: msql_srv::ColumnType::MYSQL_TYPE_VAR_STRING,
                colflags: msql_srv::ColumnFlags::empty(),
            };
            col_writer.push(col);

            let mut rw = results.start(&col_writer).unwrap();

            rw.write_col(format!("{:?}", level)).unwrap();

            rw.end_row().unwrap();

            return rw.finish();
        } else if let Some(sql_lc_strip) = sql_lc.strip_prefix("read ") {
            self.dis_kv.read_strategy = sql_lc_strip.into();
            return results.completed(1, 0);
        } else if let Some(sql_lc_strip) = sql_lc.strip_prefix("set consistency ") {
            self.dis_kv.set_consistency(sql_lc_strip);
            return results.completed(1, 0);
        } else if sql_lc.starts_with("reset") {
            println!("reseting everything");
            self.dis_kv = Default::default();
            self.ser_kv = Default::default();
            self.loading_done = false;
            self.current_session_id = None;
            self.n_sql = 0;

            return results.completed(1, 0);
        } else if sql_lc.starts_with("print summary") {
            self.dis_kv.print_summary();

            return results.completed(1, 0);
        }

        let re = Regex::new(r"(E|e)-\d+").unwrap();
        let sql = re.replace(sql, "").clone();

        let re = Regex::new("/\\*.*\\*/").unwrap();
        let sql = re.replace(&sql, "").clone();

        let re = Regex::new("\\\\\'").unwrap();
        let sql = re.replace(&sql, "").clone();

        let re = Regex::new("`([a-zA-Z0-9]+)`").unwrap();
        let sql = re.replace(&sql, "$1").clone();

        let is_primary_key_auto_increment = true;

        let parsed_sql = Parser::parse_sql(&dialect::AnsiDialect {}, &sql)
            .or_else(|_| Parser::parse_sql(&dialect::GenericDialect {}, &sql))
            .or_else(|_| Parser::parse_sql(&dialect::MsSqlDialect {}, &sql))
            .or_else(|_| Parser::parse_sql(&dialect::MySqlDialect {}, &sql))
            .or_else(|_| Parser::parse_sql(&dialect::PostgreSqlDialect {}, &sql));

        if self.loading_done {
            self.n_sql += 1;

            println!("[SQL THREAD_ID] {}", session_id);
            println!("[SQL COUNT] {}", self.n_sql);
            println!(
                "[SQL QUERY {} {:?}] {}",
                session_id, self.current_session_id, sql
            );
        }

        // println!("{:?}", parsed_sql);

        match parsed_sql {
            Ok(v) => match &v[0] {
                Query(query) => self.select(session_id, query, results),
                Insert {
                    table_name,
                    columns,
                    source,
                } => {
                    self.set_ongoing_session(session_id);
                    let columns: Vec<_> = columns.iter().map(|x| x.value.clone()).collect();
                    self.insert(session_id, table_name, &columns, source, results)
                }
                Update {
                    table_name,
                    assignments,
                    selection,
                } => {
                    self.set_ongoing_session(session_id);
                    self.update(session_id, table_name, assignments, selection, results)
                }
                Delete {
                    table_name,
                    selection,
                } => {
                    self.set_ongoing_session(session_id);
                    self.delete(session_id, table_name, selection, results)
                }
                CreateTable {
                    name,
                    columns,
                    constraints,
                    with_options,
                    external,
                    file_format,
                    location,
                    ..
                } => {
                    self.set_ongoing_session(session_id);
                    self.create_table(
                        session_id,
                        name,
                        columns,
                        constraints,
                        with_options,
                        external,
                        file_format,
                        location,
                        is_primary_key_auto_increment,
                        results,
                    )
                }
                SetVariable { .. } | Drop { .. } | CreateIndex { .. } => results.completed(1, 0),
                StartTransaction { .. } => {
                    // *last_txn_id = self.start_transaction(vec![*last_txn_id]);
                    // self.thread_id = Some(thread_id);
                    self.set_ongoing_session(session_id);
                    results.completed(1, 0)
                }
                Commit { .. } => {
                    self.commit(session_id);
                    self.remove_ongoing_session();
                    results.completed(1, 0)
                }
                Rollback { .. } => {
                    self.rollback(session_id);
                    self.remove_ongoing_session();
                    results.completed(1, 0)
                }
                e => unimplemented!("{:?}", e),
            },
            Err(e) => {
                panic!("error while parsing sql query {} - {}", sql, e);
            }
        }
    }

    fn commit(&mut self, s_id: &SessionId) -> bool {
        if self.loading_done {
            self.dis_kv.commit(s_id)
        } else {
            self.ser_kv.commit(s_id)
        }
    }

    fn rollback(&mut self, s_id: &SessionId) -> bool {
        if self.loading_done {
            self.dis_kv.rollback(s_id)
        } else {
            self.ser_kv.rollback(s_id)
        }
    }

    fn insert_with_name(&mut self, name: &str, x: &VarId, s_id: &SessionId) -> bool {
        if self.loading_done {
            self.dis_kv.insert_with_name(name, x, s_id)
        } else {
            self.ser_kv.insert_with_name(name, x, s_id)
        }
    }

    fn delete_with_name(&mut self, name: &str, x: &VarId, s_id: &SessionId) -> bool {
        if self.loading_done {
            self.dis_kv.delete_with_name(name, x, s_id)
        } else {
            self.ser_kv.delete_with_name(name, x, s_id)
        }
    }

    fn scan_with_name(&mut self, name: &str, s_id: &SessionId) -> Vec<VarId> {
        if self.loading_done {
            self.dis_kv.scan_with_name(name, s_id)
        } else {
            self.ser_kv.scan_with_name(name, s_id)
        }
    }

    fn read<K: Debug + Hash, V: DeserializeOwned>(&mut self, k: &K, s_id: &SessionId) -> Option<V> {
        // println!("reading {:?}", k);
        if self.loading_done {
            self.dis_kv.read(k, s_id)
        } else {
            self.ser_kv.read(k, s_id)
        }
    }

    fn write<K: Debug + Hash, V: Debug + Serialize>(
        &mut self,
        k: &K,
        v: &V,
        s_id: &SessionId,
    ) -> bool {
        // println!("writing {:?}", k);
        if self.loading_done {
            self.dis_kv.write(k, v, s_id)
        } else {
            self.ser_kv.write(k, v, s_id)
        }
    }

    pub fn select<W: std::io::Write>(
        &mut self,
        s_id: &SessionId,
        query: &ast::Query,
        results: QueryResultWriter<W>,
    ) -> std::io::Result<()> {
        // println!("{:?}", query);
        match query.body {
            ast::SetExpr::Select(ref s) => {
                if s.projection.iter().any(|x| match x {
                    ast::SelectItem::UnnamedExpr(ast::Expr::Identifier(_)) => false,
                    ast::SelectItem::ExprWithAlias { .. } => true,
                    ast::SelectItem::Wildcard => false,
                    ast::SelectItem::UnnamedExpr(_) => false,
                    _ => unreachable!(),
                }) {
                    let mut col_writer = Vec::new();

                    for proj in &s.projection {
                        if let ast::SelectItem::ExprWithAlias { alias, .. } = proj {
                            let col = msql_srv::Column {
                                table: "".into(),
                                column: alias.value.clone(),
                                coltype: msql_srv::ColumnType::MYSQL_TYPE_VAR_STRING,
                                colflags: msql_srv::ColumnFlags::empty(),
                            };
                            col_writer.push(col);
                        }
                    }

                    let mut rw = results.start(&col_writer).unwrap();

                    for proj in &s.projection {
                        if let ast::SelectItem::ExprWithAlias { alias, .. } = proj {
                            // TODO : also handle STOCK_COUNT
                            let val = match alias.value.as_str() {
                                "auto_increment_increment" => "1",
                                "character_set_client" => "utf8",
                                "character_set_connection" => "utf8",
                                "character_set_results" => "utf8",
                                "character_set_server" => "utf8mb4",
                                "collation_server" => "utf8mb4_0900_ai_ci",
                                "collation_connection" => "utf8_general_ci",
                                "init_connect" => "",
                                "interactive_timeout" => "28800",
                                "license" => "GPL",
                                "lower_case_table_names" => "0",
                                "max_allowed_packet" => "67108864",
                                "net_buffer_length" => "16384",
                                "net_write_timeout" => "60",
                                "sql_mode" => "ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION",
                                "system_time_zone" => "UTC",
                                "time_zone" => "SYSTEM",
                                "wait_timeout" => "28800",
                                "language" => "eng",
                                "query_cache_size" => "",
                                "query_cache_type" => "",
                                "transaction_isolation" => "SERIALIZATION",
                                "performance_schema" => "1",
                                e => unreachable!("NEW COLUMN {}", e),
                            };
                            // println!("{} : {}", proj, val);
                            rw.write_col(val).unwrap();
                        }
                    }

                    rw.end_row().unwrap();

                    return rw.finish();
                }

                self.set_ongoing_session(s_id);

                let table_name = match &s.from[0].relation {
                    ast::TableFactor::Table { name, .. } => name.0[0].clone(),
                    _ => unimplemented!(),
                };

                let projections: Vec<_> = match s.projection[0] {
                    ast::SelectItem::Wildcard => {
                        let mut cols: Vec<(String, DataType)> = self
                            .read(&(&table_name.value, KeyTag::Columns), s_id)
                            .unwrap();
                        cols.drain(..).map(|x| x.0).collect()
                    }
                    _ => s
                        .projection
                        .iter()
                        .map(|x| match x {
                            ast::SelectItem::UnnamedExpr(ast::Expr::Identifier(name)) => {
                                name.value.clone()
                            }
                            a => unimplemented!("{:?}", a),
                        })
                        .collect(),
                };

                let prim_key_set: HashSet<u64> = self
                    .scan_with_name(&table_name.value, s_id)
                    .drain(..)
                    .collect();

                let mut cols_type: Vec<_> = self
                    .read(&(&table_name.value, KeyTag::Columns), s_id)
                    .unwrap();

                let cols_type: HashMap<String, DataType> = cols_type.drain(..).collect();

                // println!("{:?}", cols_type);

                let mut col_writer = Vec::new();

                for proj in &projections {
                    let col = msql_srv::Column {
                        table: table_name.value.clone(),
                        column: proj.clone(),
                        coltype: match cols_type.get(&proj.clone()).unwrap() {
                            DataType::Int => msql_srv::ColumnType::MYSQL_TYPE_LONG,
                            DataType::Real => msql_srv::ColumnType::MYSQL_TYPE_FLOAT,
                            DataType::Float(_) => msql_srv::ColumnType::MYSQL_TYPE_FLOAT,
                            DataType::Double => msql_srv::ColumnType::MYSQL_TYPE_DOUBLE,
                            DataType::Boolean => msql_srv::ColumnType::MYSQL_TYPE_BIT,
                            DataType::Varchar(_) => msql_srv::ColumnType::MYSQL_TYPE_VAR_STRING,
                            DataType::Char(_) => msql_srv::ColumnType::MYSQL_TYPE_VARCHAR,
                            DataType::Decimal(_, _) => msql_srv::ColumnType::MYSQL_TYPE_DECIMAL,
                            DataType::Timestamp => msql_srv::ColumnType::MYSQL_TYPE_TIMESTAMP,
                        },
                        colflags: msql_srv::ColumnFlags::empty(),
                    };
                    col_writer.push(col);
                }

                let mut rw = results.start(&col_writer).unwrap();

                let mut prim_key_sorted: Vec<_> = prim_key_set.iter().cloned().collect();
                prim_key_sorted.sort_unstable();

                // println!("sorted {:?}", prim_key_sorted.len());

                for e_key in prim_key_sorted.drain(..) {
                    let mut selected = true;
                    if let Some(ref selection) = s.selection {
                        let mut select_vars = Default::default();
                        selection_vars(selection, &mut select_vars);
                        let mut select_vars_value = HashMap::new();
                        for proj in &select_vars {
                            let val: String = self
                                .read(&(&table_name.value, KeyTag::ColumnKey, proj, e_key), s_id)
                                .unwrap();
                            select_vars_value.insert(proj.clone(), val);
                        }
                        selected = selection_eval(selection, &select_vars_value)
                            .parse()
                            .unwrap();
                    }

                    // for proj in &projections {
                    //     let val: String = self
                    //         .read(
                    //             &(&table_name.value, KeyTag::ColumnKey, proj, e_key),
                    //             *last_txn_id,
                    //         )
                    //         .unwrap();
                    // }

                    if selected {
                        for proj in &projections {
                            let val: Option<String> = self
                                .read(&(&table_name.value, KeyTag::ColumnKey, proj, e_key), s_id)
                                .and_then(|x| if x == "NULL" { None } else { Some(x) });
                            rw.write_col(val).unwrap();
                        }
                        rw.end_row().unwrap();
                    }
                }

                rw.finish()
            }
            _ => unimplemented!(),
        }
    }
    pub fn insert<W: std::io::Write>(
        &mut self,
        s_id: &SessionId,
        table_name: &ast::ObjectName,
        columns: &[String],
        source: &ast::Query,
        results: QueryResultWriter<W>,
    ) -> std::io::Result<()> {
        let table_name = table_name.0[0].clone();
        if let ast::SetExpr::Values(v) = &source.body {
            let cols: Vec<(String, DataType)> = self
                .read(&(&table_name.value, KeyTag::Columns), s_id)
                .unwrap();
            let columns: Vec<_> = if columns.is_empty() {
                cols.iter().map(|e| &e.0).cloned().collect()
            } else {
                columns.to_vec()
            };
            for col in columns.iter() {
                assert!(cols.iter().any(|e| &e.0 == col));
            }
            let prim_key: Option<Vec<String>> = self
                .read(&(&table_name.value, KeyTag::PrimaryKey), s_id)
                .unwrap();

            let mut needs_to_auto_incremented = false;

            let prim_key_val = if let Some(ref prim_key_col) = prim_key {
                let mut prim_key_val: Vec<u64> = prim_key_col
                    .iter()
                    .filter_map(|prim_key_col| {
                        let mut prim_key_val = None;
                        for (i, col) in columns.iter().enumerate() {
                            if prim_key_col == col {
                                prim_key_val = match &v.0[0][i] {
                                    ast::Expr::Value(ast::Value::Number(val)) => {
                                        val.clone().parse().ok()
                                    }
                                    ast::Expr::Value(ast::Value::SingleQuotedString(val)) => {
                                        let mut s =
                                            std::collections::hash_map::DefaultHasher::new();
                                        val.hash(&mut s);
                                        Some(s.finish())
                                    }
                                    ast::Expr::Identifier(val) => val.value.parse().ok(),
                                    _ => unimplemented!("{:?}", &v.0[0][i]),
                                };
                                break;
                            }
                        }
                        prim_key_val
                    })
                    .collect();

                let last_pk: Option<u64> = self
                    .read(&(&table_name.value, KeyTag::LastPrimaryKey), s_id)
                    .unwrap();

                println!(
                    "pk table {}, autoinc {:?}, primkey vec {:?}",
                    table_name.value, last_pk, prim_key_val
                );

                if prim_key_val.is_empty() && last_pk.is_none() {
                    panic!("primary key is not provided");
                }

                if let Some(last_pk_unwrap) = last_pk {
                    if prim_key_val.is_empty() {
                        needs_to_auto_incremented = true;
                        prim_key_val.push(last_pk_unwrap + 1);
                    }
                }

                self.write(
                    &(&table_name.value, KeyTag::LastPrimaryKey),
                    &last_pk.map(|x| x + 1),
                    s_id,
                );

                let mut hasher = DefaultHasher::new();
                prim_key_val.hash(&mut hasher);
                hasher.finish()
            } else {
                rand::random()
            };

            for (i, col) in columns.iter().enumerate() {
                let val = match &v.0[0][i] {
                    ast::Expr::Value(ast::Value::Number(val)) => val.clone(),
                    ast::Expr::Value(ast::Value::SingleQuotedString(val)) => val.clone(),
                    ast::Expr::Value(ast::Value::Null) => "NULL".to_owned(),
                    ast::Expr::Identifier(val) => val.value.clone(),
                    // TODO: handle other op cases
                    ast::Expr::UnaryOp { op, expr: k } if op == &ast::UnaryOperator::Minus => {
                        format!("-{}", k)
                    }
                    _ => unimplemented!("{:?}", &v.0[0][i]),
                };

                self.write(
                    &(&table_name.value, KeyTag::ColumnKey, col, prim_key_val),
                    &val,
                    s_id,
                );
            }

            if needs_to_auto_incremented {
                println!("I am here !!!");
                let prim_key = &prim_key.unwrap()[0];
                let val: Option<u64> = self
                    .read(&(&table_name.value, KeyTag::LastPrimaryKey), s_id)
                    .unwrap();
                let val = val.unwrap();
                self.write(
                    &(&table_name.value, KeyTag::ColumnKey, prim_key, prim_key_val),
                    &val.to_string(),
                    s_id,
                );
            }

            self.insert_with_name(&table_name.value, &prim_key_val, s_id);
        }
        results.completed(1, 0)
    }
    pub fn update<W: std::io::Write>(
        &mut self,
        s_id: &SessionId,
        table_name: &ast::ObjectName,
        assignments: &[ast::Assignment],
        selection: &Option<ast::Expr>,
        results: QueryResultWriter<W>,
    ) -> std::io::Result<()> {
        let prim_key_set: HashSet<u64> = self
            .scan_with_name(&table_name.0[0].value, s_id)
            .drain(..)
            .collect();

        for e_key in prim_key_set.iter() {
            let mut selected = true;

            if let Some(ref selection) = selection {
                let mut select_vars = Default::default();
                selection_vars(selection, &mut select_vars);
                let mut select_vars_value = HashMap::new();
                for proj in &select_vars {
                    let val: String = self
                        .read(
                            &(&table_name.0[0].value, KeyTag::ColumnKey, proj, e_key),
                            s_id,
                        )
                        .unwrap();

                    select_vars_value.insert(proj.clone(), val);
                }
                selected = selection_eval(selection, &select_vars_value)
                    .parse()
                    .unwrap();
            }

            if selected {
                for assign in assignments {
                    let col = assign.id.clone();
                    // println!("{:?}", assign);
                    let val = match &assign.value {
                        ast::Expr::Identifier(x) => x.value.clone(),
                        ast::Expr::Value(ast::Value::Number(x)) => x.into(),
                        ast::Expr::Value(ast::Value::SingleQuotedString(x)) => x.into(),
                        ast::Expr::BinaryOp { left, op, right } => {
                            let left_val = match **left {
                                ast::Expr::Identifier(ref ident) => {
                                    let left_col = ident.value.clone();
                                    let val: String = self
                                        .read(
                                            &(
                                                &table_name.0[0].value,
                                                KeyTag::ColumnKey,
                                                left_col.clone(),
                                                e_key,
                                            ),
                                            s_id,
                                        )
                                        .unwrap();
                                    val
                                }
                                ast::Expr::Value(ast::Value::Number(ref x)) => x.clone(),
                                ast::Expr::UnaryOp {
                                    op: ref op1,
                                    expr: ref k,
                                } if op1 == &sqlparser::ast::UnaryOperator::Minus => {
                                    format!("-{}", k)
                                }
                                _ => unreachable!(),
                            };
                            let right_val = match **right {
                                ast::Expr::Identifier(ref ident) => {
                                    let left_col = ident.value.clone();
                                    let val: String = self
                                        .read(
                                            &(
                                                &table_name.0[0].value,
                                                KeyTag::ColumnKey,
                                                left_col.clone(),
                                                e_key,
                                            ),
                                            s_id,
                                        )
                                        .unwrap();
                                    val
                                }
                                ast::Expr::Value(ast::Value::Number(ref x)) => x.clone(),
                                ast::Expr::UnaryOp {
                                    op: ref op1,
                                    expr: ref k,
                                } if op1 == &sqlparser::ast::UnaryOperator::Minus => {
                                    format!("-{}", k)
                                }
                                _ => unreachable!(),
                            };
                            match op {
                                ast::BinaryOperator::Plus => (left_val.parse::<f64>().unwrap()
                                    + right_val.parse::<f64>().unwrap())
                                .to_string(),
                                ast::BinaryOperator::Minus => (left_val.parse::<f64>().unwrap()
                                    - right_val.parse::<f64>().unwrap())
                                .to_string(),
                                _ => unreachable!(),
                            }
                        }
                        ast::Expr::UnaryOp { op: op1, expr: k }
                            if op1 == &sqlparser::ast::UnaryOperator::Minus =>
                        {
                            format!("-{}", k)
                        }
                        _ => unimplemented!("{:?}", assign.value),
                    };

                    self.write(
                        &(&table_name.0[0].value, KeyTag::ColumnKey, col.value, e_key),
                        &val,
                        s_id,
                    );
                }
            }
        }
        // let (a, b): (Option<KeyTag>, _) = self.read(&(&table_name, KeyTag::PrimaryKey), vec![self.last_op_id]);
        results.completed(1, 0)
    }
    pub fn delete<W: std::io::Write>(
        &mut self,
        s_id: &SessionId,
        table_name: &ast::ObjectName,
        selection: &Option<ast::Expr>,
        results: QueryResultWriter<W>,
    ) -> std::io::Result<()> {
        let mut prim_key_set: HashSet<u64> = self
            .scan_with_name(&table_name.0[0].value, s_id)
            .drain(..)
            .collect();

        prim_key_set.retain(|&e_key| {
            let mut selected = true;

            if let Some(ref selection) = selection {
                let mut select_vars = Default::default();
                selection_vars(selection, &mut select_vars);
                let mut select_vars_value = HashMap::new();
                for proj in &select_vars {
                    let val: String = self
                        .read(
                            &(&table_name.0[0].value, KeyTag::ColumnKey, proj, e_key),
                            s_id,
                        )
                        .unwrap();

                    select_vars_value.insert(proj.clone(), val);
                }
                selected = selection_eval(selection, &select_vars_value)
                    .parse()
                    .unwrap();
                selected = !selected;
            }

            !selected
        });

        for prim_key in prim_key_set.iter() {
            self.delete_with_name(&table_name.0[0].value, prim_key, s_id);
        }
        results.completed(1, 0)
    }

    #[allow(clippy::too_many_arguments)]
    pub fn create_table<W: std::io::Write>(
        &mut self,
        s_id: &SessionId,
        table_name: &ast::ObjectName,
        columns: &[ast::ColumnDef],
        constraints: &[ast::TableConstraint],
        _with_options: &[ast::SqlOption],
        _external: &bool,
        _file_format: &Option<ast::FileFormat>,
        _location: &Option<String>,
        is_pk_auto_increment: bool,
        results: QueryResultWriter<W>,
    ) -> std::io::Result<()> {
        let table_name = table_name.0[0].clone();

        // writing primary key set

        let columns: Vec<_> = columns
            .iter()
            .map(|e| {
                let data_type = match e.data_type {
                    ast::DataType::Int => DataType::Int,
                    ast::DataType::SmallInt => DataType::Int,
                    ast::DataType::Real => DataType::Real,
                    ast::DataType::Float(a) => DataType::Float(a),
                    ast::DataType::Double => DataType::Double,
                    ast::DataType::Boolean => DataType::Boolean,
                    ast::DataType::Varchar(n) => DataType::Varchar(n),
                    ast::DataType::Char(n) => DataType::Char(n),
                    ast::DataType::Decimal(a, b) => DataType::Decimal(a, b),
                    ast::DataType::Timestamp => DataType::Timestamp,
                    ast::DataType::BigInt => DataType::Int,
                    ast::DataType::Custom(ref obj) => match obj {
                        ast::ObjectName(v) => match v[0].value.to_lowercase().as_ref() {
                            "tinyint" => DataType::Int,
                            "smallint" => DataType::Int,
                            "datetime" => DataType::Varchar(None),
                            _ => unimplemented!("{:?}", v),
                        },
                    },
                    ast::DataType::Text => DataType::Varchar(None),
                    ref e => unimplemented!("{:?}", e),
                };
                (&e.name.value, data_type)
            })
            .collect();

        // writing table columns

        self.write(&(&table_name.value, KeyTag::Columns), &columns, s_id);
        let mut prim_key: Option<Vec<String>> = None;
        for e in constraints.iter() {
            match e {
                ast::TableConstraint::Unique { columns, .. } => {
                    prim_key = Some(
                        columns
                            .iter()
                            .map(|x| &x.value)
                            .cloned()
                            .collect::<Vec<_>>(),
                    );
                }
                ast::TableConstraint::ForeignKey { .. } => {}
                _ => unimplemented!("{:?}", e),
            }
        }

        for e in constraints.iter() {
            match e {
                ast::TableConstraint::Unique {
                    columns,
                    is_primary,
                    ..
                } => {
                    if *is_primary {
                        prim_key = Some(
                            columns
                                .iter()
                                .map(|x| &x.value)
                                .cloned()
                                .collect::<Vec<_>>(),
                        );
                    }
                }
                ast::TableConstraint::ForeignKey { .. } => {}
                _ => unimplemented!("{:?}", e),
            }
        }

        let mut last_pk = None;

        if is_pk_auto_increment {
            last_pk = Some(0u64);
        }

        self.write(&(&table_name.value, KeyTag::LastPrimaryKey), &last_pk, s_id);

        // if prim_key == None {
        //     prim_key = Some(&columns[0].0);
        // }

        // println!("{} - prim key {:?}", table_name.value, prim_key);

        // write table primary key column

        self.write(&(&table_name.value, KeyTag::PrimaryKey), &prim_key, s_id);
        results.completed(1, 0)
    }
}
