use mysql::prelude::*;
use mysql::*;

fn main() {
    let url: String = "mysql://root@127.0.0.1:3306".into();

    let mut conn0 = Conn::new(&url).unwrap();
    conn0
        .query_drop(r"CREATE TABLE kv (var int, val int, primary key (var));")
        .unwrap();
    //
    // conn0
    //     .query_drop(r#"insert into kv (var, val) values (1, 0);"#)
    //     .unwrap();

    // conn0
    //     .query_drop(r#"insert into kv (var, val) values (2, 0);"#)
    //     .unwrap();

    // let mut m: HashMap<_, u64> = HashMap::new();

    println!("setup done");

    let mut conn0 = Conn::new(&url).unwrap();
    conn0.query_drop("loading done").unwrap();

    // let c1: u64 = {
    //     let mut conn2 = Conn::new(&url).unwrap();
    //     let c: u64 = conn2
    //         .query_first(r#"select val from kv where var=1;"#)
    //         .unwrap()
    //         .unwrap();
    //     conn2
    //         .query_drop(format!("update kv set val=1 where var={};", c + 1))
    //         .unwrap();
    //     conn2
    //         .query_first(r#"select val from kv where var=1;"#)
    //         .unwrap()
    //         .unwrap()
    // };

    // let c2: u64 = {
    //     let mut conn2 = Conn::new(&url).unwrap();
    //     let c: u64 = conn2
    //         .query_first(r#"select val from kv where var=1;"#)
    //         .unwrap()
    //         .unwrap();
    //     conn2
    //         .query_drop(format!("update kv set val=1 where var={};", c + 1))
    //         .unwrap();
    //     conn2
    //         .query_first(r#"select val from kv where var=1;"#)
    //         .unwrap()
    //         .unwrap()
    // };
    //
    // *m.entry((c1, c2)).or_default() += 1;
    //
    // println!("{:?}", m);

    // Test read committed consistency

    let _c3: u64 = {
        let mut conn2 = Conn::new(&url).unwrap();
        conn2
            .query_drop(r#"insert into kv (var, val) values (100, 5);"#)
            .unwrap();
        conn2
            .query_drop(format!("update kv set val=7 where var={};", 100))
            .unwrap();
        1 // dummy return
    };

    let c4: (u64, u64) = {
        let mut conn2 = Conn::new(&url).unwrap();
        let mut tx = conn2.start_transaction(TxOpts::default()).unwrap();
        let res1 = tx
            .query_first(r#"select val from kv where var=100;"#)
            .unwrap();
        let res2 = tx
            .query_first(r#"select val from kv where var=100;"#)
            .unwrap();
        let _ = tx.commit().is_ok();

        let mut read1 = 0;
        let mut read2 = 0;

        if res1 != None {
            read1 = res1.unwrap();
        }
        if res2 != None {
            read2 = res2.unwrap();
        }
        (read1, read2)
    };

    println!("{:?}", c4);

    let mut conn3 = Conn::new(&url).unwrap();
    conn3.query_drop("dump history").unwrap();

    // conn.query_drop(r"update student set age=30 where id=1")
    //     .unwrap();
    // conn.query_drop(r"select id, name, age from student")
    //     .unwrap();
    // conn.query_drop(r"delete from student where id=1").unwrap();
    // conn.query_drop(r"select id, name, age from student")
    //     .unwrap();
}
