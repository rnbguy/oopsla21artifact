use monkeydb::server::ConcObj;
use monkeydb::sql::SQLdb;
use std::net;

use std::sync::{Arc, Condvar, Mutex};

use msql_srv::MysqlIntermediary;

use std::thread;

use clap::Parser;

#[derive(Parser)]
struct Opts {
    #[clap(short, long, default_value = "3306")]
    anyport: u16,
}

fn main() {
    let opts: Opts = Opts::parse();

    // listen to sql default port 3306
    let listener = net::TcpListener::bind(format!("127.0.0.1:{}", opts.anyport)).unwrap();

    let port = listener.local_addr().unwrap().port();

    println!("port:{}", port);

    let shared_obj = Arc::new((Mutex::new(SQLdb::default()), Condvar::new()));

    let mut sessions = Vec::new();

    let mut curr_session_id = 0;

    while let Ok((stream, _)) = listener.accept() {
        println!("new connection");
        curr_session_id += 1;
        let obj = shared_obj.clone();
        let t = thread::spawn(move || {
            let obj = ConcObj::from_sqldb(curr_session_id, obj);
            MysqlIntermediary::run_on_tcp(obj, stream).unwrap();
            println!("serving done");
        });
        sessions.push(t);
    }

    sessions.drain(..).for_each(|jh| jh.join().unwrap());
}
