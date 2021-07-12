use mysql::prelude::*;
use mysql::Conn;

use std::collections::HashMap;

use clap::Clap;

fn cr01(conn: &mut Conn) -> bool {
    /*
    SELECT * FROM
    (SELECT AVG(bal) as AVG_SAVINGS FROM SAVINGS) as s
    JOIN
    (SELECT AVG(bal) as AVG_CHECKING FROM CHECKING) as c
    WHERE (s.AVG_SAVINGS + c.AVG_CHECKING) = 5000;
    */
    let mut net_balance = HashMap::new();

    let result = conn.query_iter("SELECT custid, bal FROM SAVINGS").unwrap();

    for mut row in result.flatten() {
        let custid: u64 = row.take::<String, _>("custid").unwrap().parse().unwrap();
        let bal: f64 = row.take::<String, _>("bal").unwrap().parse().unwrap();

        net_balance.insert(custid, bal);
    }

    let result = conn.query_iter("SELECT custid, bal FROM CHECKING").unwrap();

    for mut row in result.flatten() {
        let custid: u64 = row.take::<String, _>("custid").unwrap().parse().unwrap();
        let bal: f64 = row.take::<String, _>("bal").unwrap().parse().unwrap();

        let ent = net_balance.entry(custid).or_default();
        *ent += bal;
    }

    // println!("{:?}", net_balance);

    (net_balance.values().sum::<f64>() - (2500. + 2500.) * (net_balance.len() as f64)).abs() < f64::EPSILON
}

fn cr02(conn: &mut Conn) -> bool {
    /*
    SELECT * FROM SAVINGS WHERE bal < 0;
    */
    let mut savings_balance = HashMap::new();

    let result = conn.query_iter("SELECT custid, bal FROM SAVINGS").unwrap();

    for mut row in result.flatten() {
            let custid: u64 = row.take::<String, _>("custid").unwrap().parse().unwrap();
            let bal: f64 = row.take::<String, _>("bal").unwrap().parse().unwrap();

            savings_balance.insert(custid, bal);
    }

    savings_balance.values().all(|x| x >= &0.)
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
                    // A13 for smallbank
                    println!("assert_id {} is violated (after {} tries and {:.2} secs)", i + 1 + 12, *cnt_ent, *dur_ent);
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
    let url: String = format!("mysql://root@127.0.0.1:{}/smallbank", opts.port);
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
