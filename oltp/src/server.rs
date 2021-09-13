use std::io;
use std::sync::{Arc, Condvar, Mutex};

use super::sql::SQLdb;

use msql_srv::{InitWriter, MysqlShim, ParamParser, QueryResultWriter, StatementMetaWriter};

use crate::kv::SessionId;

pub struct ConcObj {
    session_id: SessionId,
    _autocommit: bool,
    obj: Arc<(Mutex<SQLdb>, Condvar)>,
}

impl<'a> ConcObj {
    pub fn from_sqldb(session_id: SessionId, obj: Arc<(Mutex<SQLdb>, Condvar)>) -> Self {
        ConcObj {
            session_id,
            _autocommit: false,
            obj,
        }
    }
}

impl<W> MysqlShim<W> for ConcObj
where
    W: io::Write,
{
    type Error = io::Error;

    fn on_prepare(&mut self, _: &str, info: StatementMetaWriter<W>) -> io::Result<()> {
        // println!("prepare {}", s);
        info.reply(42, &[], &[])
    }
    fn on_execute(
        &mut self,
        k: u32,
        _p: ParamParser,
        results: QueryResultWriter<W>,
    ) -> io::Result<()> {
        println!("executed {}", k);
        results.completed(1, 0)
    }
    fn on_close(&mut self, k: u32) {
        println!("closing {}", k);
    }

    fn on_init(&mut self, s: &str, writer: InitWriter<W>) -> io::Result<()> {
        println!("init {}", s);
        writer.ok()
    }

    fn on_query(&mut self, query: &str, results: QueryResultWriter<W>) -> io::Result<()> {
        // use rand::Rng;
        // let mut rng = rand::thread_rng();
        // std::thread::sleep(std::time::Duration::from_millis(rng.gen::<u64>() % 10));

        println!("query {}", query);
        let (sqldb_mutex, cvar) = &*self.obj;

        let mut sqldb = sqldb_mutex.lock().unwrap();

        if sqldb.is_ongoing_session(self.session_id) {
            sqldb = cvar
                .wait_while(sqldb, |sqldb| sqldb.is_ongoing_session(self.session_id))
                .unwrap();
        }

        let rt = sqldb.execute(&self.session_id, query, results);

        if sqldb.no_ongoing_session() {
            // println!("it was commit, so unlocking {}", self.session_id);
            cvar.notify_one();
        }

        rt
    }
}
