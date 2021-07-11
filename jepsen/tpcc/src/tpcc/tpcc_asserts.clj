(ns tpcc.tpcc-asserts)

(def assertions [
;; cr01
"SELECT * FROM
(SELECT W_ID, W_YTD FROM WAREHOUSE) AS w
JOIN (SELECT D_W_ID, SUM(D_YTD) AS d_ytd_agg FROM DISTRICT GROUP BY D_W_ID) AS d
ON (w.W_ID = d.D_W_ID) WHERE w.W_YTD != d.d_ytd_agg;",
;; cr02
"SELECT * FROM
DISTRICT AS d
JOIN (SELECT O_W_ID, O_D_ID, MAX(O_ID) AS max_o_id FROM OORDER GROUP BY O_W_ID, O_D_ID) AS o
ON (d.D_W_ID = o.O_W_ID AND d.D_ID = o.O_D_ID)
JOIN (SELECT NO_W_ID, NO_D_ID, MAX(NO_O_ID) AS max_no_id FROM NEW_ORDER GROUP BY NO_W_ID,NO_D_ID ) AS no
ON (o.O_W_ID = no.NO_W_ID AND o.O_D_ID = no.NO_D_ID) WHERE (d.D_NEXT_O_ID - 1 != o.max_o_id OR o.max_o_id != no.max_no_id);",
;; cr03
"SELECT (no.max_no - no.min_no) + 1 - no.cnt_no FROM
(SELECT MIN(NO_O_ID) AS min_no, MAX(NO_O_ID) AS max_no, COUNT(*) AS cnt_no,
NO_W_ID, NO_D_ID FROM NEW_ORDER GROUP BY NO_W_ID, NO_D_ID) AS no
WHERE (no.max_no - no.min_no) + 1 != no.cnt_no;",
;; cr04
"SELECT * FROM
(SELECT O_W_ID, O_D_ID, SUM(O_OL_CNT) AS o_ol_cnt_agg FROM OORDER GROUP BY
O_W_ID, O_D_ID) AS o
JOIN (SELECT OL_W_ID, OL_D_ID, count(*) AS ol_cnt FROM ORDER_LINE GROUP BY
OL_W_ID, OL_D_ID) AS ol
ON (o.O_W_ID = ol.OL_W_ID) AND (o.O_D_ID = ol.OL_D_ID)
WHERE o.o_ol_cnt_agg != ol.ol_cnt;",
;; cr05
"SELECT * FROM
OORDER AS o
JOIN NEW_ORDER AS no
ON ((o.O_W_ID = no.NO_W_ID) AND (o.O_D_ID = no.NO_D_ID) AND (o.O_ID = no.NO_O_ID))
WHERE o.O_CARRIER_ID IS NOT NULL;",
;; cr06
"SELECT * FROM
OORDER AS o
JOIN (SELECT OL_W_ID, OL_D_ID, OL_O_ID, count(*) AS ol_cnt FROM ORDER_LINE GROUP BY
OL_W_ID, OL_D_ID, OL_O_ID) AS ol
ON (o.O_W_ID = ol.OL_W_ID) AND (o.O_D_ID = ol.OL_D_ID) AND (o.O_ID = ol.OL_O_ID)
WHERE o.O_OL_CNT != ol.ol_cnt;",
;; cr07
"SELECT * FROM
(SELECT * FROM ORDER_LINE WHERE OL_DELIVERY_D IS NULL) AS ol
JOIN OORDER AS o
ON (o.O_W_ID = ol.OL_W_ID) AND (o.O_D_ID = ol.OL_D_ID) AND (o.O_ID = ol.OL_O_ID)
WHERE o.O_CARRIER_ID IS NOT NULL;",
;; cr08
"SELECT * FROM
WAREHOUSE AS w
JOIN (SELECT H_W_ID, sum(H_AMOUNT) AS h_amt_agg FROM HISTORY GROUP BY H_W_ID)
AS h
on (w.W_ID = h.H_W_ID)
WHERE w.W_YTD != h.h_amt_agg;",
;; cr09
"SELECT * FROM
DISTRICT AS d
JOIN (SELECT H_W_ID, H_D_ID, sum(H_AMOUNT) AS h_amt_agg FROM HISTORY GROUP BY
H_W_ID, H_D_ID) AS h
on (d.D_W_ID = h.H_W_ID) AND (d.D_ID = h.H_D_ID)
WHERE d.D_YTD != h.h_amt_agg;",
;; cr10
"SELECT c.C_BALANCE, ool.ol_amt_agg, h.h_amt_agg, (ool.ol_amt_agg - h.h_amt_agg - c.C_BALANCE) FROM
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
WHERE (ool.ol_amt_agg - h.h_amt_agg) != c.C_BALANCE;",
;; cr11
"SELECT (o.o_cnt - no.no_cnt) FROM
CUSTOMER AS c
JOIN (SELECT O_W_ID, O_D_ID, count(*) AS o_cnt FROM OORDER GROUP BY O_W_ID, O_D_ID)
AS o
ON (o.O_W_ID = c.C_W_ID) AND (o.O_D_ID = c.C_D_ID)
LEFT JOIN (SELECT NO_W_ID, NO_D_ID, count(*) AS no_cnt FROM NEW_ORDER GROUP BY
NO_W_ID, NO_D_ID) AS no
ON (o.O_W_ID = no.NO_W_ID) AND (o.O_D_ID = no.NO_D_ID)
WHERE (o.o_cnt - no.no_cnt) > 2; -- 2100",
;; cr12
"SELECT c.C_BALANCE, c.C_YTD_PAYMENT, ol.ol_amt_agg , (c.C_BALANCE + c.C_YTD_PAYMENT - ol.ol_amt_agg) FROM
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
WHERE c.C_BALANCE + c.C_YTD_PAYMENT != ool.ol_amt_agg;",
])
