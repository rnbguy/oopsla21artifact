(ns tpcc.tpcc-utils
       (:require [clojure.tools.logging :refer :all])
)

;; https://github.com/Kiarahmani/Jepsen_Java_Tests/tree/makingTPCC/src/main/java/tpcc
;; https://www.cockroachlabs.com/docs/stable/export.html
;; https://www.cockroachlabs.com/docs/stable/import.html

(def operationMap [

                   {:n 1, :f :NO-TXN,
                          :javaFunc (fn [conn args] (tpcc.NewOrder/newOrder conn (nth args 0)(nth args 1)(nth args 2)(nth args 3)(nth args 4)(nth args 5)(nth args 6)(nth args 7))),
                          :freq 45/100}
                   {:n 2, :f :PM-TXN,
                          :javaFunc (fn [conn args] (tpcc.Payment/payment conn   (nth args 0)(nth args 1)(nth args 2)(nth args 3)(nth args 4)(nth args 5)(nth args 6)(nth args 7))),
                          :freq 43/100}
                   {:n 3, :f :OS-TXN,
                          :javaFunc (fn [conn args] (tpcc.OrderStatus/orderStatus conn (nth args 0)(nth args 1)(nth args 2)(nth args 3)(nth args 4))),
                          :freq 4/100}
                   {:n 4, :f :DV-TXN,
                          :javaFunc (fn [conn args] (tpcc.Delivery/delivery conn (nth args 0)(nth args 1))),
                          :freq 4/100}
                   {:n 5, :f :SL-TXN,
                          :javaFunc (fn [conn args] (tpcc.StockLevel/stockLevel conn (nth args 0)(nth args 1)(nth args 2))),
                          :freq 4/100}])


;====================================================================================================
;

(defn getNextArgs
  "generates input arguments for the requested transaction"
  [txnNo]
  (condp = txnNo
    1 (let [w_id      (tpcc.Utils_tpcc/get_w_id)
            d_id      (tpcc.Utils_tpcc/get_d_id)
            c_id      (tpcc.Utils_tpcc/get_c_id)
            num_items (tpcc.Utils_tpcc/get_num_items)
            wh_sup_and_lcl_list (tpcc.Utils_tpcc/get_sup_wh_and_o_all_local num_items w_id)
            wh_sup    (nth wh_sup_and_lcl_list 0)
            all_local (nth wh_sup_and_lcl_list 1)
            item_ids  (tpcc.Utils_tpcc/get_item_ids num_items)
            order_qnts(tpcc.Utils_tpcc/get_order_quantities num_items)]
        [w_id,d_id,c_id,all_local,num_items,item_ids,wh_sup,order_qnts])

    2 (let [w_id                (tpcc.Utils_tpcc/get_w_id)
            d_id                (tpcc.Utils_tpcc/get_d_id)
            payment_cust        (tpcc.Utils_tpcc/get_payment_cust)
            customerByName      (nth payment_cust 0)
            c_id                (nth payment_cust 1)
            c_last              (nth payment_cust 2)
            cust_info           (tpcc.Utils_tpcc/get_customerinfo w_id d_id)
            customerWarehouseID (nth cust_info 0)
            customerDistrictID  (nth cust_info 1)
            paymentAmount       (tpcc.Utils_tpcc/get_paymentAmount)]
        [w_id,d_id,customerByName,c_id,c_last,customerWarehouseID,customerDistrictID,paymentAmount])

    3 (let [w_id                (tpcc.Utils_tpcc/get_w_id)
            d_id                (tpcc.Utils_tpcc/get_d_id)
            orderStatus_cust    (tpcc.Utils_tpcc/get_orderStatus_cust)
            customerByName      (nth orderStatus_cust 0)
            c_id                (nth orderStatus_cust 1)
            c_last              (nth orderStatus_cust 2)]
        [w_id,d_id,customerByName,c_id,c_last])
    4 (let [w_id          (tpcc.Utils_tpcc/get_w_id)
            o_carrier_id  (tpcc.Utils_tpcc/get_o_carrier_id)]
       [w_id, o_carrier_id])
    5 (let [w_id      (tpcc.Utils_tpcc/get_w_id)
            d_id      (tpcc.Utils_tpcc/get_d_id)
            threshold (tpcc.Utils_tpcc/get_threshold)]
        [w_id, d_id, threshold])
    (info "ERROR!! ---> UNKNOWN txnNo")))


