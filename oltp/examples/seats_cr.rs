use mysql::prelude::*;
use mysql::Conn;

use std::collections::{HashMap, HashSet};

use clap::Clap;

fn cr01(conn: &mut Conn) -> bool {
    let mut reservations: HashMap<_, HashSet<_>> = HashMap::new();

    let mut result = conn
        .query_iter("SELECT R_ID, R_F_ID, R_SEAT FROM RESERVATION")
        .unwrap();

    result.all(|row| {
        if let Ok(mut row) = row {
            let f_id: u64 = row.take::<String, _>("R_F_ID").unwrap().parse().unwrap();
            let r_seat: u64 = row.take::<String, _>("R_SEAT").unwrap().parse().unwrap();

            let ent = reservations.entry(f_id).or_default();
            ent.insert(r_seat)
        } else {
            true
        }
    })
}

fn do_check(conn: &mut Conn, asserts: &[fn(&mut Conn) -> bool], n: usize) {
    let mut cnt_map = vec![0isize; asserts.len()];
    let mut dur_map = vec![0f32; asserts.len()];
    for _ in 0..n {
        for (i, curr_assert) in asserts.iter().enumerate() {
            let cnt_ent = cnt_map.get_mut(i).unwrap();
            if *cnt_ent <= 0 {
                *cnt_ent -= 1;
                let begin = std::time::Instant::now();
                let ans = !curr_assert(conn);
                conn.query_drop("ROLLBACK").unwrap();
                let dur_ent = dur_map.get_mut(i).unwrap();
                *dur_ent += begin.elapsed().as_secs_f32();
                if ans {
                    *cnt_ent = -*cnt_ent;
                    println!("cr {} {} {:.2}", i + 1, *cnt_ent, *dur_ent);
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
}

fn main() {
    let opts: Opts = Opts::parse();
    let url: String = format!("mysql://root@127.0.0.1:{}/seats", opts.port);
    let mut conn = Conn::new(&url).unwrap();

    let asserts: Vec<fn(&mut Conn) -> bool> = vec![cr01];

    println!(
        "{}",
        conn.query_first::<String, _>("check consistency")
            .unwrap()
            .unwrap()
    );

    conn.query_drop(format!("read {}", opts.strategy)).unwrap();
    do_check(&mut conn, &asserts, 5);
}
