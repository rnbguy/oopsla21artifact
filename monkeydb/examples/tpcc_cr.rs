use mysql::prelude::*;
use mysql::Conn;

use std::collections::{HashMap, HashSet};

use mysql::Value;

use clap::Clap;

fn cr01(conn: &mut Conn) -> bool {
    // district wise ytd is same on WAREHOUSE and DISTRICT

    /*
    SELECT * FROM
    (SELECT W_ID, W_YTD FROM WAREHOUSE) AS w
    JOIN (SELECT D_W_ID, SUM(D_YTD) AS d_ytd_agg FROM DISTRICT GROUP BY D_W_ID) AS d
    ON (w.W_ID = d.D_W_ID) WHERE w.W_YTD != d.d_ytd_agg;
    */

    let mut warehouse = HashMap::new();
    let mut district: HashMap<_, f64> = HashMap::new();

    let result = conn
        .query_iter("SELECT W_ID, W_YTD FROM WAREHOUSE")
        .unwrap();

    for mut row in result.flatten() {
        let w_id: u64 = row.take::<String, _>("W_ID").unwrap().parse().unwrap();
        let w_ytd: f64 = row.take::<String, _>("W_YTD").unwrap().parse().unwrap();

        warehouse.insert(w_id, w_ytd);
    }

    let result = conn
        .query_iter("SELECT D_W_ID, D_YTD FROM DISTRICT")
        .unwrap();

    for mut row in result.flatten() {
        let d_w_id: u64 = row.take::<String, _>("D_W_ID").unwrap().parse().unwrap();
        let d_ytd: f64 = row.take::<String, _>("D_YTD").unwrap().parse().unwrap();

        let entry = district.entry(d_w_id).or_default();
        *entry += d_ytd;
    }

    warehouse
        .keys()
        .filter(|&v| (warehouse[v] - district[v]).abs() > 1e-8)
        .count()
        == 0
}

fn cr02(conn: &mut Conn) -> bool {
    // next order id for (district, warehouse) are same on OORDER and NEW_ORDER

    /*
    SELECT * FROM
    DISTRICT AS d
    JOIN (SELECT O_W_ID, O_D_ID, MAX(O_ID) AS max_o_id FROM OORDER GROUP BY O_W_ID, O_D_ID) AS o
    ON (d.D_W_ID = o.O_W_ID AND d.D_ID = o.O_D_ID)
    JOIN (SELECT NO_W_ID, NO_D_ID, MAX(NO_O_ID) AS max_no_id FROM NEW_ORDER GROUP BY NO_W_ID,NO_D_ID ) AS no
    ON (o.O_W_ID = no.NO_W_ID AND o.O_D_ID = no.NO_D_ID) WHERE (d.D_NEXT_O_ID - 1 != o.max_o_id OR o.max_o_id != no.max_no_id);
    */

    let mut district = HashMap::new();
    let mut oorder = HashMap::new();
    let mut new_order = HashMap::new();

    let result = conn
        .query_iter("SELECT D_W_ID, D_ID, D_NEXT_O_ID FROM DISTRICT")
        .unwrap();

    for mut row in result.flatten() {
        let d_w_id: u64 = row.take::<String, _>("D_W_ID").unwrap().parse().unwrap();
        let d_id: u64 = row.take::<String, _>("D_ID").unwrap().parse().unwrap();
        let d_next_o_id: u64 = row
            .take::<String, _>("D_NEXT_O_ID")
            .unwrap()
            .parse()
            .unwrap();

        district.insert((d_w_id, d_id), d_next_o_id);
    }

    let result = conn
        .query_iter("SELECT O_W_ID, O_D_ID, O_ID FROM OORDER")
        .unwrap();

    for mut row in result.flatten() {
        let o_w_id: u64 = row.take::<String, _>("O_W_ID").unwrap().parse().unwrap();
        let o_d_id: u64 = row.take::<String, _>("O_D_ID").unwrap().parse().unwrap();
        let o_id: u64 = row.take::<String, _>("O_ID").unwrap().parse().unwrap();

        let entry = oorder.entry((o_w_id, o_d_id)).or_insert_with(|| o_id);

        if o_id > *entry {
            *entry = o_id;
        }
    }

    let result = conn
        .query_iter("SELECT NO_W_ID, NO_D_ID, NO_O_ID FROM NEW_ORDER")
        .unwrap();

    for mut row in result.flatten() {
        let no_w_id: u64 = row.take::<String, _>("NO_W_ID").unwrap().parse().unwrap();
        let no_d_id: u64 = row.take::<String, _>("NO_D_ID").unwrap().parse().unwrap();
        let no_o_id: u64 = row.take::<String, _>("NO_O_ID").unwrap().parse().unwrap();

        let entry = new_order
            .entry((no_w_id, no_d_id))
            .or_insert_with(|| no_o_id);

        if no_o_id > *entry {
            *entry = no_o_id;
        }
    }

    district
        .keys()
        .filter(|&v| {
            if let Some(max_o_id) = oorder.get(v) {
                if &(district[v] - 1) != max_o_id {
                    return true;
                }
                if let Some(max_no_id) = new_order.get(v) {
                    if max_o_id != max_no_id {
                        return true;
                    }
                }
            }
            false
        })
        .count()
        == 0
}

fn cr03(conn: &mut Conn) -> bool {
    // NEWORDER has order in sequence

    /*
    SELECT (no.max_no - no.min_no) + 1 - no.cnt_no FROM
    (SELECT MIN(NO_O_ID) AS min_no, MAX(NO_O_ID) AS max_no, COUNT(*) AS cnt_no,
    NO_W_ID, NO_D_ID FROM NEW_ORDER GROUP BY NO_W_ID, NO_D_ID) AS no
    WHERE (no.max_no - no.min_no) + 1 != no.cnt_no;
    */

    let mut new_order = HashMap::new();

    let result = conn
        .query_iter("SELECT NO_W_ID, NO_D_ID, NO_O_ID FROM NEW_ORDER")
        .unwrap();

    for mut row in result.flatten() {
        let no_w_id: u64 = row.take::<String, _>("NO_W_ID").unwrap().parse().unwrap();
        let no_d_id: u64 = row.take::<String, _>("NO_D_ID").unwrap().parse().unwrap();
        let no_o_id: u64 = row.take::<String, _>("NO_O_ID").unwrap().parse().unwrap();

        let entry = new_order
            .entry((no_w_id, no_d_id))
            .or_insert_with(|| (no_o_id, no_o_id, 0));

        if no_o_id < entry.0 {
            entry.0 = no_o_id;
        } else if no_o_id > entry.1 {
            entry.1 = no_o_id;
        }

        entry.2 += 1;
    }

    new_order
        .values()
        .filter(|&&(min_no, max_no, cnt_no)| (max_no - min_no) + 1 != cnt_no)
        .count()
        == 0
}

fn cr04(conn: &mut Conn) -> bool {
    /*
    SELECT * FROM
    (SELECT O_W_ID, O_D_ID, SUM(O_OL_CNT) AS o_ol_cnt_agg FROM OORDER GROUP BY
    O_W_ID, O_D_ID) AS o
    JOIN (SELECT OL_W_ID, OL_D_ID, count(*) AS ol_cnt FROM ORDER_LINE GROUP BY
    OL_W_ID, OL_D_ID) AS ol
    ON (o.O_W_ID = ol.OL_W_ID) AND (o.O_D_ID = ol.OL_D_ID)
    WHERE o.o_ol_cnt_agg != ol.ol_cnt;
    */

    let mut oorder: HashMap<_, u64> = HashMap::new();
    let mut order_line: HashMap<_, u64> = HashMap::new();

    let result = conn
        .query_iter("SELECT O_W_ID, O_D_ID, O_OL_CNT FROM OORDER")
        .unwrap();

    for mut row in result.flatten() {
        let o_w_id: u64 = row.take::<String, _>("O_W_ID").unwrap().parse().unwrap();
        let o_d_id: u64 = row.take::<String, _>("O_D_ID").unwrap().parse().unwrap();
        let o_ol_cnt: u64 = row.take::<String, _>("O_OL_CNT").unwrap().parse().unwrap();

        let entry = oorder.entry((o_w_id, o_d_id)).or_default();
        *entry += o_ol_cnt;
    }

    let result = conn
        .query_iter("SELECT OL_W_ID, OL_D_ID FROM ORDER_LINE")
        .unwrap();

    for mut row in result.flatten() {
        let ol_w_id: u64 = row.take::<String, _>("OL_W_ID").unwrap().parse().unwrap();
        let ol_d_id: u64 = row.take::<String, _>("OL_D_ID").unwrap().parse().unwrap();

        let entry = order_line.entry((ol_w_id, ol_d_id)).or_default();
        *entry += 1;
    }

    let keys: HashSet<_> = oorder.keys().cloned().collect();
    let keys: HashSet<_> = keys
        .intersection(&order_line.keys().cloned().collect())
        .cloned()
        .collect();

    keys.iter().filter(|&v| oorder[v] != order_line[v]).count() == 0
}

fn cr05(conn: &mut Conn) -> bool {
    // no order is pending on OORDER and NEW_ORDER

    /*
    SELECT * FROM
    OORDER AS o
    JOIN NEW_ORDER AS no
    ON ((o.O_W_ID = no.NO_W_ID) AND (o.O_D_ID = no.NO_D_ID) AND (o.O_ID = no.NO_O_ID))
    WHERE o.O_CARRIER_ID IS NOT NULL;
    */

    let mut oorder = HashSet::new();
    let mut new_order = HashSet::new();

    let result = conn
        .query_iter("SELECT O_W_ID, O_D_ID, O_ID,O_CARRIER_ID FROM OORDER")
        .unwrap();

    for mut row in result.flatten() {
        let o_w_id: u64 = row.take::<String, _>("O_W_ID").unwrap().parse().unwrap();
        let o_d_id: u64 = row.take::<String, _>("O_D_ID").unwrap().parse().unwrap();
        let o_id: u64 = row.take::<String, _>("O_ID").unwrap().parse().unwrap();
        let o_carrier_id: Value = row.take("O_CARRIER_ID").unwrap();

        if o_carrier_id != Value::NULL && o_carrier_id != Value::Bytes("NULL".as_bytes().to_vec()) {
            oorder.insert((o_w_id, o_d_id, o_id));
        }
    }

    let result = conn
        .query_iter("SELECT NO_W_ID, NO_D_ID, NO_O_ID FROM NEW_ORDER")
        .unwrap();

    for mut row in result.flatten() {
        let no_w_id: u64 = row.take::<String, _>("NO_W_ID").unwrap().parse().unwrap();
        let no_d_id: u64 = row.take::<String, _>("NO_D_ID").unwrap().parse().unwrap();
        let no_id: u64 = row.take::<String, _>("NO_O_ID").unwrap().parse().unwrap();

        new_order.insert((no_w_id, no_d_id, no_id));
    }

    oorder.intersection(&new_order).count() == 0
}

fn cr06(conn: &mut Conn) -> bool {
    // order_line and open_order must be same

    /*
    SELECT * FROM
    OORDER AS o
    JOIN (SELECT OL_W_ID, OL_D_ID, OL_O_ID, count(*) AS ol_cnt FROM ORDER_LINE GROUP BY
    OL_W_ID, OL_D_ID, OL_O_ID) AS ol
    ON (o.O_W_ID = ol.OL_W_ID) AND (o.O_D_ID = ol.OL_D_ID) AND (o.O_ID = ol.OL_O_ID)
    WHERE o.O_OL_CNT != ol.ol_cnt;
    */

    let mut oorder = HashMap::new();
    let mut order_line: HashMap<_, u64> = HashMap::new();

    let result = conn
        .query_iter("SELECT O_W_ID, O_D_ID, O_ID, O_OL_CNT FROM OORDER")
        .unwrap();

    for mut row in result.flatten() {
        let o_w_id: u64 = row.take::<String, _>("O_W_ID").unwrap().parse().unwrap();
        let o_d_id: u64 = row.take::<String, _>("O_D_ID").unwrap().parse().unwrap();
        let o_id: u64 = row.take::<String, _>("O_ID").unwrap().parse().unwrap();
        let o_ol_cnt: u64 = row.take::<String, _>("O_OL_CNT").unwrap().parse().unwrap();

        oorder.insert((o_w_id, o_d_id, o_id), o_ol_cnt);
    }

    let result = conn
        .query_iter("SELECT OL_W_ID, OL_D_ID, OL_O_ID FROM ORDER_LINE")
        .unwrap();

    for mut row in result.flatten() {
        let ol_w_id: u64 = row.take::<String, _>("OL_W_ID").unwrap().parse().unwrap();
        let ol_d_id: u64 = row.take::<String, _>("OL_D_ID").unwrap().parse().unwrap();
        let ol_o_id: u64 = row.take::<String, _>("OL_O_ID").unwrap().parse().unwrap();

        let entry = order_line.entry((ol_w_id, ol_d_id, ol_o_id)).or_default();

        *entry += 1;
    }

    let keys: HashSet<_> = oorder.keys().cloned().collect();
    let keys: HashSet<_> = keys
        .intersection(&order_line.keys().cloned().collect())
        .cloned()
        .collect();

    keys.iter().filter(|&v| oorder[v] != order_line[v]).count() == 0
}

fn cr07(conn: &mut Conn) -> bool {
    //
    /*
    SELECT * FROM
    (SELECT * FROM ORDER_LINE WHERE OL_DELIVERY_D IS NULL) AS ol
    JOIN OORDER AS o
    ON (o.O_W_ID = ol.OL_W_ID) AND (o.O_D_ID = ol.OL_D_ID) AND (o.O_ID = ol.OL_O_ID)
    WHERE o.O_CARRIER_ID IS NOT NULL;
    */

    let mut oorder = HashMap::new();
    let mut order_line = HashMap::new();

    let result = conn
        .query_iter("SELECT O_W_ID, O_D_ID, O_ID, O_CARRIER_ID FROM OORDER")
        .unwrap();

    for mut row in result.flatten() {
        let o_w_id: u64 = row.take::<String, _>("O_W_ID").unwrap().parse().unwrap();
        let o_d_id: u64 = row.take::<String, _>("O_D_ID").unwrap().parse().unwrap();
        let o_id: u64 = row.take::<String, _>("O_ID").unwrap().parse().unwrap();
        let o_carrier_id = row.take_opt::<String, _>("O_CARRIER_ID").unwrap().is_ok();

        oorder.insert((o_w_id, o_d_id, o_id), o_carrier_id);
    }

    let result = conn
        .query_iter("SELECT OL_W_ID, OL_D_ID, OL_O_ID, OL_DELIVERY_D FROM ORDER_LINE")
        .unwrap();

    for mut row in result.flatten() {
        let ol_w_id: u64 = row.take::<String, _>("OL_W_ID").unwrap().parse().unwrap();
        let ol_d_id: u64 = row.take::<String, _>("OL_D_ID").unwrap().parse().unwrap();
        let ol_o_id: u64 = row.take::<String, _>("OL_O_ID").unwrap().parse().unwrap();
        let ol_delivery_id = row.take_opt::<String, _>("OL_DELIVERY_D").unwrap().is_ok();

        order_line.insert((ol_w_id, ol_d_id, ol_o_id), ol_delivery_id);
    }

    let keys: HashSet<_> = oorder.keys().cloned().collect();
    let keys: HashSet<_> = keys
        .intersection(&order_line.keys().cloned().collect())
        .cloned()
        .collect();

    keys.iter().filter(|&v| oorder[v] != order_line[v]).count() == 0
}

fn cr08(conn: &mut Conn) -> bool {
    /*
    SELECT * FROM
    WAREHOUSE AS w
    JOIN (SELECT H_W_ID, sum(H_AMOUNT) AS h_amt_agg FROM HISTORY GROUP BY H_W_ID)
    AS h
    on (w.W_ID = h.H_W_ID)
    WHERE w.W_YTD != h.h_amt_agg;
    */

    let mut warehouse = HashMap::new();
    let mut history: HashMap<_, f64> = HashMap::new();

    let result = conn
        .query_iter("SELECT W_ID, W_YTD FROM WAREHOUSE")
        .unwrap();

    for mut row in result.flatten() {
        {
            let w_id: u64 = row.take::<String, _>("W_ID").unwrap().parse().unwrap();
            let w_ytd: f64 = row.take::<String, _>("W_YTD").unwrap().parse().unwrap();

            warehouse.insert(w_id, w_ytd);
        }
    }

    let result = conn
        .query_iter("SELECT H_W_ID, H_AMOUNT FROM HISTORY")
        .unwrap();

    for mut row in result.flatten() {
        {
            let h_w_id: u64 = row.take::<String, _>("H_W_ID").unwrap().parse().unwrap();
            let h_amount: f64 = row.take::<String, _>("H_AMOUNT").unwrap().parse().unwrap();

            let entry = history.entry(h_w_id).or_default();
            *entry += h_amount;
        }
    }

    let keys: HashSet<_> = warehouse.keys().cloned().collect();
    let keys: HashSet<_> = keys
        .intersection(&history.keys().cloned().collect())
        .cloned()
        .collect();

    keys.iter()
        .filter(|&v| (warehouse[v] - history[v]).abs() > 1e-8)
        .count()
        == 0
}

fn cr09(conn: &mut Conn) -> bool {
    /*
    SELECT * FROM
    DISTRICT AS d
    JOIN (SELECT H_W_ID, H_D_ID, sum(H_AMOUNT) AS h_amt_agg FROM HISTORY GROUP BY
    H_W_ID, H_D_ID) AS h
    on (d.D_W_ID = h.H_W_ID) AND (d.D_ID = h.H_D_ID)
    WHERE d.D_YTD != h.h_amt_agg;
    */

    let mut district = HashMap::new();
    let mut history: HashMap<_, f64> = HashMap::new();

    let result = conn
        .query_iter("SELECT D_W_ID, D_ID, D_YTD FROM DISTRICT")
        .unwrap();

    for mut row in result.flatten() {
        {
            let d_w_id: u64 = row.take::<String, _>("D_W_ID").unwrap().parse().unwrap();
            let d_id: u64 = row.take::<String, _>("D_ID").unwrap().parse().unwrap();
            let d_ytd: f64 = row.take::<String, _>("D_YTD").unwrap().parse().unwrap();

            district.insert((d_w_id, d_id), d_ytd);
        }
    }

    let result = conn
        .query_iter("SELECT H_W_ID, H_D_ID, H_AMOUNT FROM HISTORY")
        .unwrap();

    for mut row in result.flatten() {
        {
            let h_w_id: u64 = row.take::<String, _>("H_W_ID").unwrap().parse().unwrap();
            let h_d_id: u64 = row.take::<String, _>("H_D_ID").unwrap().parse().unwrap();
            let h_amount: f64 = row.take::<String, _>("H_AMOUNT").unwrap().parse().unwrap();

            let entry = history.entry((h_w_id, h_d_id)).or_default();
            *entry += h_amount;
        }
    }

    let keys: HashSet<_> = district.keys().cloned().collect();
    let keys: HashSet<_> = keys
        .intersection(&history.keys().cloned().collect())
        .cloned()
        .collect();
    keys.iter()
        .filter(|&v| (district[v] - history[v]).abs() > 1e-8)
        .count()
        == 0
}

fn cr10(conn: &mut Conn) -> bool {
    /*
    SELECT c.C_BALANCE, ool.ol_amt_agg, h.h_amt_agg, (ool.ol_amt_agg - h.h_amt_agg - c.C_BALANCE) FROM
    (SELECT C_W_ID, C_D_ID, C_ID, C_BALANCE FROM CUSTOMER) AS c
    JOIN
    (SELECT H_C_W_ID, H_C_D_ID, H_C_ID, SUM(H_AMOUNT) AS h_amt_agg FROM HISTORY GROUP BY H_C_W_ID, H_C_D_ID, H_C_ID) AS h
    JOIN
    (SELECT O_W_ID, O_D_ID, O_C_ID, SUM(OL_AMOUNT) AS ol_amt_agg FROM (
        OORDER AS o
        JOIN
        (SELECT OL_W_ID, OL_D_ID, OL_O_ID, OL_AMOUNT FROM
    ORDER_LINE WHERE OL_DELIVERY_D IS NOT NULL) AS ol
    ON ((ol.OL_W_ID = o.O_W_ID) AND (ol.OL_D_ID = o.O_D_ID) AND (ol.OL_O_ID = o.O_ID)))
    GROUP BY O_W_ID, O_D_ID, O_C_ID) AS ool
    ON (c.C_W_ID = h.H_C_W_ID) AND (c.C_D_ID = h.H_C_D_ID) AND (c.C_ID = h.H_C_ID) AND
    (c.C_W_ID = ool.O_W_ID) AND (c.C_D_ID = ool.O_D_ID) AND (c.C_ID = ool.O_C_ID)
    WHERE (ool.ol_amt_agg - h.h_amt_agg) != c.C_BALANCE;
    */

    let mut order = HashMap::new();
    let mut customer = HashMap::new();
    let mut history: HashMap<_, f64> = HashMap::new();
    let mut order_line: HashMap<_, f64> = HashMap::new();

    let result = conn
        .query_iter("SELECT O_ID, O_W_ID, O_D_ID, O_C_ID FROM OORDER")
        .unwrap();

    for mut row in result.flatten() {
        {
            let o_id: u64 = row.take::<String, _>("O_ID").unwrap().parse().unwrap();
            let o_w_id: u64 = row.take::<String, _>("O_W_ID").unwrap().parse().unwrap();
            let o_d_id: u64 = row.take::<String, _>("O_D_ID").unwrap().parse().unwrap();
            let o_c_id: u64 = row.take::<String, _>("O_C_ID").unwrap().parse().unwrap();

            order.insert((o_id, o_w_id, o_d_id), o_c_id);
        }
    }

    let result = conn
        .query_iter("SELECT C_ID, C_W_ID, C_D_ID, C_BALANCE FROM CUSTOMER")
        .unwrap();

    for mut row in result.flatten() {
        {
            let c_id: u64 = row.take::<String, _>("C_ID").unwrap().parse().unwrap();
            let c_w_id: u64 = row.take::<String, _>("C_W_ID").unwrap().parse().unwrap();
            let c_d_id: u64 = row.take::<String, _>("C_D_ID").unwrap().parse().unwrap();
            let c_balance: f64 = row.take::<String, _>("C_BALANCE").unwrap().parse().unwrap();

            customer.insert((c_id, c_w_id, c_d_id), c_balance);
        }
    }

    let result = conn
        .query_iter("SELECT H_C_W_ID, H_C_D_ID, H_C_ID, H_AMOUNT FROM HISTORY")
        .unwrap();

    for mut row in result.flatten() {
        {
            let h_c_w_id: u64 = row.take::<String, _>("H_C_W_ID").unwrap().parse().unwrap();
            let h_c_d_id: u64 = row.take::<String, _>("H_C_D_ID").unwrap().parse().unwrap();
            let h_c_id: u64 = row.take::<String, _>("H_C_ID").unwrap().parse().unwrap();
            let h_amount: f64 = row.take::<String, _>("H_AMOUNT").unwrap().parse().unwrap();

            let entry = history.entry((h_c_id, h_c_w_id, h_c_d_id)).or_default();

            *entry += h_amount;
        }
    }

    let result = conn
        .query_iter("SELECT OL_W_ID, OL_D_ID, OL_O_ID, OL_AMOUNT, OL_DELIVERY_D FROM ORDER_LINE")
        .unwrap();

    for mut row in result.flatten() {
        {
            let ol_w_id: u64 = row.take::<String, _>("OL_W_ID").unwrap().parse().unwrap();
            let ol_d_id: u64 = row.take::<String, _>("OL_D_ID").unwrap().parse().unwrap();
            let ol_o_id: u64 = row.take::<String, _>("OL_O_ID").unwrap().parse().unwrap();
            let ol_amount: f64 = row.take::<String, _>("OL_AMOUNT").unwrap().parse().unwrap();
            let ol_delivery_d: Value = row.take("OL_DELIVERY_D").unwrap();

            if ol_delivery_d != Value::NULL
                && ol_delivery_d != Value::Bytes("NULL".as_bytes().to_vec())
            {
                let o_c_id = *order.get(&(ol_o_id, ol_w_id, ol_d_id)).unwrap();

                let entry = order_line.entry((o_c_id, ol_w_id, ol_d_id)).or_default();

                *entry += ol_amount;
            }
        }
    }

    let keys: HashSet<(u64, u64, u64)> = customer.keys().cloned().collect();
    let keys: HashSet<(u64, u64, u64)> = keys
        .intersection(&history.keys().cloned().collect())
        .cloned()
        .collect();
    let keys: HashSet<(u64, u64, u64)> = keys
        .intersection(&order_line.keys().cloned().collect())
        .cloned()
        .collect();

    keys.iter()
        .filter(|&v| (customer[v] - order_line[v] + history[v]).abs() > 1e-8)
        .count()
        == 0
}

fn cr11(conn: &mut Conn) -> bool {
    /*
    SELECT (o.o_cnt - no.no_cnt) FROM
    CUSTOMER AS c
    JOIN (SELECT O_W_ID, O_D_ID, count(*) AS o_cnt FROM OORDER GROUP BY O_W_ID, O_D_ID)
    AS o
    ON (o.O_W_ID = c.C_W_ID) AND (o.O_D_ID = c.C_D_ID)
    LEFT JOIN (SELECT NO_W_ID, NO_D_ID, count(*) AS no_cnt FROM NEW_ORDER GROUP BY
    NO_W_ID, NO_D_ID) AS no
    ON (o.O_W_ID = no.NO_W_ID) AND (o.O_D_ID = no.NO_D_ID)
    WHERE (o.o_cnt - no.no_cnt) > 2100;
    */

    // let mut customer = HashSet::new();
    let mut order: HashMap<_, u64> = HashMap::new();
    let mut new_order: HashMap<_, u64> = HashMap::new();

    // let result = conn
    //     .query_iter("SELECT C_W_ID, C_D_ID FROM CUSTOMER")
    //     .unwrap();

    // for mut row in result.flatten() {
    //     {
    //         let c_w_id: u64 = row.take::<String, _>("C_W_ID").unwrap().parse().unwrap();
    //         let c_d_id: u64 = row.take::<String, _>("C_D_ID").unwrap().parse().unwrap();

    //         customer.insert((c_w_id, c_d_id));
    //     }
    // }

    let result = conn
        .query_iter("SELECT O_W_ID, O_D_ID FROM OORDER")
        .unwrap();

    for mut row in result.flatten() {
        {
            let o_w_id: u64 = row.take::<String, _>("O_W_ID").unwrap().parse().unwrap();
            let o_d_id: u64 = row.take::<String, _>("O_D_ID").unwrap().parse().unwrap();

            // if customer.contains(&(o_w_id, o_d_id)) {
            let entry = order.entry((o_w_id, o_d_id)).or_default();
            *entry += 1;
            // }
        }
    }

    let result = conn
        .query_iter("SELECT NO_W_ID, NO_D_ID FROM NEW_ORDER")
        .unwrap();

    for mut row in result.flatten() {
        {
            let no_w_id: u64 = row.take::<String, _>("NO_W_ID").unwrap().parse().unwrap();
            let no_d_id: u64 = row.take::<String, _>("NO_D_ID").unwrap().parse().unwrap();

            // if customer.contains(&(no_w_id, no_d_id)) {
            let entry = new_order.entry((no_w_id, no_d_id)).or_default();
            *entry += 1;
            // }
        }
    }

    let keys: HashSet<_> = order.keys().cloned().collect();
    let keys: HashSet<_> = keys
        .intersection(&new_order.keys().cloned().collect())
        .cloned()
        .collect();

    keys.iter().filter(|&v| order[v] - new_order[v] < 2).count() == 0
}

fn cr12(conn: &mut Conn) -> bool {
    /*
    SELECT c.C_BALANCE, c.C_YTD_PAYMENT, ol.ol_amt_agg , (c.C_BALANCE + c.C_YTD_PAYMENT - ol.ol_amt_agg) FROM
    CUSTOMER AS c
    JOIN OORDER AS o
    ON (o.O_W_ID = c.C_W_ID) AND (o.O_D_ID = c.C_D_ID) AND (o.O_C_ID = c.C_ID)
    JOIN (SELECT OL_W_ID, OL_D_ID, OL_O_ID, SUM(OL_AMOUNT) AS ol_amt_agg FROM
    ORDER_LINE WHERE OL_DELIVERY_D IS NOT NULL GROUP BY OL_W_ID, OL_D_ID, OL_O_ID) AS ol
    ON (o.O_W_ID = ol.OL_W_ID) AND (o.O_D_ID = ol.OL_D_ID) AND (o.O_ID = ol.OL_O_ID)
    WHERE c.C_BALANCE + c.C_YTD_PAYMENT != ol.ol_amt_agg;

    SELECT c.C_BALANCE, c.C_YTD_PAYMENT, ool.ol_amt_agg, (ool.ol_amt_agg - c.C_YTD_PAYMENT - c.C_BALANCE) FROM
    (SELECT C_W_ID, C_D_ID, C_ID, C_YTD_PAYMENT, C_BALANCE FROM CUSTOMER) AS c
    JOIN
    (SELECT O_W_ID, O_D_ID, O_C_ID, SUM(OL_AMOUNT) AS ol_amt_agg FROM (
        OORDER AS o
        JOIN
        (SELECT OL_W_ID, OL_D_ID, OL_O_ID, OL_AMOUNT FROM
    ORDER_LINE WHERE OL_DELIVERY_D IS NOT NULL) AS ol
    ON ((ol.OL_W_ID = o.O_W_ID) AND (ol.OL_D_ID = o.O_D_ID) AND (ol.OL_O_ID = o.O_ID)))
    GROUP BY O_W_ID, O_D_ID, O_C_ID) AS ool
    ON (c.C_W_ID = ool.O_W_ID) AND (c.C_D_ID = ool.O_D_ID) AND (c.C_ID = ool.O_C_ID)
    WHERE c.C_BALANCE + c.C_YTD_PAYMENT != ool.ol_amt_agg;


    */

    let mut order = HashMap::new();
    let mut customer = HashMap::new();
    let mut order_line: HashMap<_, f64> = HashMap::new();

    let result = conn
        .query_iter("SELECT O_ID, O_W_ID, O_D_ID, O_C_ID FROM OORDER")
        .unwrap();

    for mut row in result.flatten() {
            let o_id: u64 = row.take::<String, _>("O_ID").unwrap().parse().unwrap();
            let o_w_id: u64 = row.take::<String, _>("O_W_ID").unwrap().parse().unwrap();
            let o_d_id: u64 = row.take::<String, _>("O_D_ID").unwrap().parse().unwrap();
            let o_c_id: u64 = row.take::<String, _>("O_C_ID").unwrap().parse().unwrap();

            order.insert((o_id, o_w_id, o_d_id), o_c_id);
    }

    let result = conn
        .query_iter("SELECT C_ID, C_W_ID, C_D_ID, C_BALANCE, C_YTD_PAYMENT FROM CUSTOMER")
        .unwrap();

    for mut row in result.flatten() {
        let c_id: u64 = row.take::<String, _>("C_ID").unwrap().parse().unwrap();
        let c_w_id: u64 = row.take::<String, _>("C_W_ID").unwrap().parse().unwrap();
        let c_d_id: u64 = row.take::<String, _>("C_D_ID").unwrap().parse().unwrap();
        let c_balance: f64 = row.take::<String, _>("C_BALANCE").unwrap().parse().unwrap();
        let c_ytd_payment: f64 = row
            .take::<String, _>("C_YTD_PAYMENT")
            .unwrap()
            .parse()
            .unwrap();

        customer.insert((c_id, c_w_id, c_d_id), c_balance + c_ytd_payment);
    }

    let result = conn
        .query_iter("SELECT OL_W_ID, OL_D_ID, OL_O_ID, OL_AMOUNT, OL_DELIVERY_D FROM ORDER_LINE")
        .unwrap();

    for mut row in result.flatten() {
        let ol_w_id: u64 = row.take::<String, _>("OL_W_ID").unwrap().parse().unwrap();
        let ol_d_id: u64 = row.take::<String, _>("OL_D_ID").unwrap().parse().unwrap();
        let ol_o_id: u64 = row.take::<String, _>("OL_O_ID").unwrap().parse().unwrap();
        let ol_amount: f64 = row.take::<String, _>("OL_AMOUNT").unwrap().parse().unwrap();
        let ol_delivery_d: Value = row.take("OL_DELIVERY_D").unwrap();

        if ol_delivery_d != Value::NULL && ol_delivery_d != Value::Bytes("NULL".as_bytes().to_vec())
        {
            let o_c_id = *order.get(&(ol_o_id, ol_w_id, ol_d_id)).unwrap();

            let entry = order_line.entry((o_c_id, ol_w_id, ol_d_id)).or_default();

            *entry += ol_amount;
        }
    }

    let keys: HashSet<_> = customer.keys().cloned().collect();
    let keys: HashSet<_> = keys
        .intersection(&order_line.keys().cloned().collect())
        .cloned()
        .collect();

    keys.iter()
        .filter(|&v| (customer[v] - order_line[v]).abs() > 1e-8)
        .count()
        == 0
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
                    // A1-A12 for TPCC
                    println!("A{} is violated (after {} tries and {:.2} secs)", i + 1, *cnt_ent, *dur_ent);
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
    let url: String = format!("mysql://root@127.0.0.1:{}/{}", opts.port, opts.db);
    let mut conn = Conn::new(&url).unwrap();

    let asserts: Vec<fn(&mut Conn) -> bool> = vec![
        cr01, cr02, cr03, cr04, cr05, cr06, cr07, cr08, cr09, cr10, cr11, cr12,
    ];

    // println!(
    //     "{}",
    //     conn.query_first::<String, _>("check consistency")
    //         .unwrap()
    //         .unwrap()
    // );

    // conn.query_drop(format!("read {}", opts.strategy)).unwrap();
    do_check(&mut conn, &asserts, 5);
}
