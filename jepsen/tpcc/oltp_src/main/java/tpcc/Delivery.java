package tpcc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import java.sql.Connection;

public class Delivery {
	private static boolean _VERBOSE = false;

	public static int delivery(Connection conn, int w_id, int o_carrier_id) throws Exception {
		try {
			PreparedStatement stmt = null;
			PreparedStatement ol_stmt = null;
			PreparedStatement no_stmt = null;
			PreparedStatement oo_stmt = null;
			PreparedStatement cu_stmt = null;

			int d_id;
			int[] orderIDs = new int[10];
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			for (d_id = 1; d_id <= 10; d_id++) {
				stmt = conn.prepareStatement("SELECT NO_O_ID FROM " + "NEW_ORDER" + " WHERE NO_D_ID = ? "
						+ "   AND NO_W_ID = ? " + " ORDER BY no_d_id,no_o_id" + " LIMIT 1" + "");
				stmt.setInt(1, d_id);
				stmt.setInt(2, w_id);
				ResultSet no_rs = stmt.executeQuery();
				if (!no_rs.next()) {
					// This district has no new orders
					// This can happen but should be rare
					if (_VERBOSE)
						System.out.println(String.format("District has no new orders [W_ID=%d, D_ID=%d]", w_id, d_id));
					continue;
				}

				int no_o_id = no_rs.getInt("NO_O_ID");
				orderIDs[d_id - 1] = no_o_id;
				no_rs.close();
				// retrieve order
				stmt = conn.prepareStatement("SELECT O_C_ID FROM " + "OORDER" + " WHERE O_ID = ? "
						+ "   AND O_D_ID = ? " + "   AND O_W_ID = ?");
				stmt.setInt(1, no_o_id);
				stmt.setInt(2, d_id);
				stmt.setInt(3, w_id);
				ResultSet oo_rs = stmt.executeQuery();
				if (!oo_rs.next()) {
					System.out.println(
							String.format("ERROR_41: Failed to retrieve ORDER record [W_ID=%d, D_ID=%d, O_ID=%d]", w_id,
									d_id, no_o_id));
					return 41;
				}
				int c_id = oo_rs.getInt("O_C_ID");
				oo_rs.close();

				// delete the row containing the oldest order
				no_stmt = conn.prepareStatement("DELETE FROM " + "NEW_ORDER" + " WHERE NO_O_ID = ? "
						+ " AND NO_D_ID = ?" + "   AND NO_W_ID = ?");

				no_stmt.setInt(1, no_o_id);
				no_stmt.setInt(2, d_id);
				no_stmt.setInt(3, w_id);
				no_stmt.executeUpdate();

				//
				// update order's carrier id
				oo_stmt = conn.prepareStatement("UPDATE OORDER  SET O_CARRIER_ID = ? " + " WHERE O_ID = ? "
						+ "   AND O_D_ID = ?" + "   AND O_W_ID = ?");
				oo_stmt.setInt(1, o_carrier_id);
				oo_stmt.setInt(2, no_o_id);
				oo_stmt.setInt(3, d_id);
				oo_stmt.setInt(4, w_id);
				oo_stmt.executeUpdate();

				//
				// retrieve and update all orderlines belonging to this order
				stmt = conn.prepareStatement("SELECT OL_NUMBER, OL_AMOUNT FROM ORDER_LINE " + " WHERE OL_O_ID = ? "
						+ "   AND OL_D_ID = ? " + "   AND OL_W_ID = ? ");
				stmt.setInt(1, no_o_id);
				stmt.setInt(2, d_id);
				stmt.setInt(3, w_id);
				ResultSet ol_rs = stmt.executeQuery();
				List<Integer> all_ol_numbers = new ArrayList<Integer>();
				// read all ol_numbers and get sum of ol_amounts
				double ol_total = 0;
				while (ol_rs.next()) {
					all_ol_numbers.add(ol_rs.getInt("OL_NUMBER"));
					ol_total += ol_rs.getDouble("OL_AMOUNT");
				}
				// update all matching rows in orderline table
				String ol_in_clause = Utils_tpcc.get_in_clause(all_ol_numbers);
				ol_stmt = conn
						.prepareStatement("UPDATE " + "ORDER_LINE" + "   SET OL_DELIVERY_D = ? " + " WHERE OL_O_ID = ? "
								+ "   AND OL_D_ID = ? " + "   AND OL_W_ID = ? " + "AND OL_NUMBER IN " + ol_in_clause);
				ol_stmt.setTimestamp(1, timestamp);
				ol_stmt.setInt(2, no_o_id);
				ol_stmt.setInt(3, d_id);
				ol_stmt.setInt(4, w_id);
				ol_stmt.executeUpdate();
				

				// retrieve customer's info
				stmt = conn.prepareStatement("SELECT  C_BALANCE, C_DELIVERY_CNT" + " FROM CUSTOMER"
						+ " WHERE C_W_ID = ? " + "   AND C_D_ID = ? " + " AND C_ID = ? ");
				stmt.setInt(1, w_id);
				stmt.setInt(2, d_id);
				stmt.setInt(3, c_id);
				ResultSet c_rs = stmt.executeQuery();
				if (!c_rs.next()) {
					System.out.println("ERROR_42: customer does not exist: " + w_id + "," + d_id + "," + c_id);
					return 42;
				}
				double c_balance = c_rs.getDouble("C_BALANCE");
				int c_delivery_cnt = c_rs.getInt("C_DELIVERY_CNT");
				c_rs.close();
				// update customer's info
				cu_stmt = conn.prepareStatement("UPDATE " + "CUSTOMER" + " SET C_BALANCE = ?," + " C_DELIVERY_CNT = ? "
						+ " WHERE C_W_ID = ? " + "   AND C_D_ID = ? " + " AND C_ID = ? ");
				cu_stmt.setDouble(1, c_balance + ol_total);
				cu_stmt.setInt(2, c_delivery_cnt + 1);
				cu_stmt.setInt(3, w_id);
				cu_stmt.setInt(4, d_id);
				cu_stmt.setInt(5, c_id);
				cu_stmt.executeUpdate();
			}
			// cu_stmt.executeBatch();
			// oo_stmt.executeBatch();
			// ol_stmt.executeBatch();
			// no_stmt.executeBatch();

			// ❄❄❄❄❄❄❄❄❄❄❄❄❄❄❄
			// TXN SUCCESSFUL!
			// ❄❄❄❄❄❄❄❄❄❄❄❄❄❄❄
			return 0;
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
	}
}
