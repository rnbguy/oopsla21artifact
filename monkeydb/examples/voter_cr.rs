use mysql::prelude::*;
use mysql::Conn;

use std::collections::HashMap;

use clap::Clap;

fn cr01(conn: &mut Conn) -> bool {
    /*
    SELECT * FROM
    (SELECT phone_number, COUNT(*) as count FROM VOTES GROUP BY phone_number) as v
    WHERE v.count > 2;
    */
    let mut vote_cnt: HashMap<_, u64> = HashMap::new();

    let result = conn.query_iter("SELECT * FROM VOTES").unwrap();

    for row in result {
        if let Ok(mut row) = row {
            let phone_number: u64 = row
                .take::<String, _>("phone_number")
                .unwrap()
                .parse()
                .unwrap();

            let ent = vote_cnt.entry(phone_number).or_default();
            *ent += 1;
        }
    }

    vote_cnt.values().all(|&x| x <= 2)
}

fn do_check(conn: &mut Conn, asserts: &[fn(&mut Conn) -> bool], n: usize) {
    let mut cnt_map = vec![0isize; asserts.len()];
    let mut dur_map = vec![0f32; asserts.len()];
    for _ in 0..n {
        for i in 0..asserts.len() {
            let cnt_ent = cnt_map.get_mut(i).unwrap();
            if *cnt_ent <= 0 {
                *cnt_ent -= 1;
                let begin = std::time::Instant::now();
                let ans = !asserts[i](conn);
                conn.query_drop("ROLLBACK").unwrap();
                let dur_ent = dur_map.get_mut(i).unwrap();
                *dur_ent += begin.elapsed().as_secs_f32();
                if ans {
                    *cnt_ent = -*cnt_ent;
                    // A14 for voter
                    println!("A{} is violated (after {} tries and {:.2} secs)", i + 1 + 13, *cnt_ent, *dur_ent);
                }
            }
        }
    }
}

#[derive(Clap)]
struct Opts {
    #[clap(short, long, default_value = "3306")]
    port: u16,
    #[clap(short, long)]
    strategy: String,
    #[clap(short, long)]
    db: String,
}

fn main() {
    let opts: Opts = Opts::parse();
    let url: String = format!("mysql://root@127.0.0.1:{}/voter", opts.port);
    let mut conn = Conn::new(&url).unwrap();

    let asserts: Vec<fn(&mut Conn) -> bool> = vec![cr01];

    // println!(
    //     "{}",
    //     conn.query_first::<String, _>("check consistency")
    //         .unwrap()
    //         .unwrap()
    // );

    // conn.query_drop(format!("read {}", opts.strategy)).unwrap();
    do_check(&mut conn, &asserts, 5);
}
