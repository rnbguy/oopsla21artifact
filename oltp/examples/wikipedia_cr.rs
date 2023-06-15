use mysql::prelude::*;
use mysql::Conn;

use std::collections::HashMap;

use clap::Parser;

fn cr01(conn: &mut Conn) -> bool {
    // Total number of changes done for all the pages by each user in recentchanges = no.of pages
    // edited by individual user useracct

    // SELECT user_id, user_editcount FROM useracct
    // SELECT rc_user, count(*) FROM recentchanges GROUP BY rc_user

    /*
    SELECT * FROM
    (SELECT user_id, user_editcount FROM useracct)
    JOIN
    (SELECT rc_user, count(*) as rc_count FROM recentchanges GROUP BY rc_user)
    ON (user_id = rc_user)
    WHERE user_editcount != rc_count;
    */

    let mut change_cnt = HashMap::new();

    let result = conn
        .query_iter("SELECT user_id, user_editcount FROM useracct")
        .unwrap();

    for mut row in result.flatten() {
        let user_id: u64 = row.take::<String, _>("user_id").unwrap().parse().unwrap();
        let user_editcount: u64 = row
            .take::<String, _>("user_editcount")
            .unwrap()
            .parse()
            .unwrap();

        change_cnt.insert(user_id, user_editcount);
    }

    let result = conn
        .query_iter("SELECT rc_user FROM recentchanges")
        .unwrap();

    for mut row in result.flatten() {
        let rc_user: u64 = row.take::<String, _>("rc_user").unwrap().parse().unwrap();

        let ent = change_cnt.entry(rc_user).or_default();
        *ent -= 1;
    }

    change_cnt.values().all(|x| x == &0)
}

fn cr02(conn: &mut Conn) -> bool {
    // Total number of changes done for all the pages by each user in logging = no.of pages edited by
    // individual user useracct

    // SELECT user_id, user_editcount FROM useracct
    // SELECT log_user, count(*) FROM logging GROUP BY log_user

    /*
    SELECT * FROM
    (SELECT user_id, user_editcount FROM useracct)
    JOIN
    (SELECT log_user, count(*) as log_count FROM logging GROUP BY log_user)
    ON (user_id = log_user)
    WHERE user_editcount != log_count;
    */

    let mut change_cnt = HashMap::new();

    let result = conn
        .query_iter("SELECT user_id, user_editcount FROM useracct")
        .unwrap();

    for mut row in result.flatten() {
        let user_id: u64 = row.take::<String, _>("user_id").unwrap().parse().unwrap();
        let user_editcount: u64 = row
            .take::<String, _>("user_editcount")
            .unwrap()
            .parse()
            .unwrap();

        change_cnt.insert(user_id, user_editcount);
    }

    let result = conn.query_iter("SELECT log_user FROM logging").unwrap();

    for mut row in result.flatten() {
        let log_user: u64 = row.take::<String, _>("log_user").unwrap().parse().unwrap();

        let ent = change_cnt.entry(log_user).or_default();
        *ent -= 1;
    }

    change_cnt.values().all(|x| x == &0)
}

fn cr03(conn: &mut Conn) -> bool {
    // Total number of changes done for all the pages by each user in logging = no.of pages edited by
    // individual user recentchanges

    // SELECT rc_user, count(*) FROM recentchanges GROUP BY rc_user
    // SELECT log_user, count(*) FROM logging GROUP BY log_user

    /*
    SELECT * FROM
    (SELECT rc_user, count(*) as rc_count FROM recentchanges GROUP BY rc_user)
    JOIN
    (SELECT log_user, count(*) as log_count FROM logging GROUP BY log_user)
    ON (rc_user = log_user)
    WHERE rc_count != log_count;
    */

    let mut change_cnt: HashMap<_, u64> = HashMap::new();

    let result = conn.query_iter("SELECT log_user FROM logging").unwrap();

    for mut row in result.flatten() {
        let log_user: u64 = row.take::<String, _>("log_user").unwrap().parse().unwrap();

        let ent = change_cnt.entry(log_user).or_default();
        *ent += 1;
    }

    let result = conn
        .query_iter("SELECT rc_user FROM recentchanges")
        .unwrap();

    for mut row in result.flatten() {
        let rc_user: u64 = row.take::<String, _>("rc_user").unwrap().parse().unwrap();

        let ent = change_cnt.entry(rc_user).or_default();
        *ent -= 1;
    }

    change_cnt.values().all(|x| x == &0)
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
                    // A15-A17 for wikipedia
                    println!(
                        "assert_id {} is violated (after {} tries and {:.2} secs)",
                        i + 1 + 14,
                        *cnt_ent,
                        *dur_ent
                    );
                }
            }
        }
    }
}

#[derive(Parser)]
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
    let url: String = format!("mysql://root@127.0.0.1:{}/{}", opts.port, opts.db);
    let mut conn = Conn::new(url).unwrap();

    let asserts: Vec<fn(&mut Conn) -> bool> = vec![cr01, cr02, cr03];

    // println!(
    //     "{}",
    //     conn.query_first::<String, _>("check consistency")
    //         .unwrap()
    //         .unwrap()
    // );

    // conn.query_drop(format!("read {}", opts.strategy)).unwrap();
    do_check(&mut conn, &asserts, 5);
}
