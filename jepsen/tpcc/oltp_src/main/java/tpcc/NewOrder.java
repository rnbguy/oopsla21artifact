package tpcc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;

import java.sql.Connection;

public class NewOrder {
	private static boolean _VERBOSE = false;

	private static void printArray(String name, int[] arr) {
		System.out.print(name + ": [");
		String delim = "";
		for (int i : arr) {
			System.out.print(delim + i);
			delim = ", ";
		}
		System.out.println("]");
	}

	public static int newOrder(Connection conn, int w_id, int d_id, int c_id, int o_all_local, int o_ol_cnt,
			int[] itemIDs, int[] supplierWarehouseIDs, int[] orderQuantities) throws Exception {
		PreparedStatement stmt = null, stmtUpdateStock = null;

		try {
			if (_VERBOSE) {
				System.out.println("******************************");
				System.out.println("w_id:                 " + w_id);
				System.out.println("d_id:                 " + d_id);
				System.out.println("c_id:                 " + c_id);
				System.out.println("o_all_local:          " + o_all_local);
				System.out.println("o_ol_cnt:             " + o_ol_cnt);
				System.out.print("itemIDs:              [");
				printArray("supplierWarehouseIDs", supplierWarehouseIDs);
				printArray("itemIDs", itemIDs);
				printArray("orderQuantities", orderQuantities);
				System.out.println("******************************");
			}

			// datastructures required for bookkeeping
			double[] itemPrices = new double[o_ol_cnt];
			String[] itemNames = new String[o_ol_cnt];
			double[] stockQuantities = new double[o_ol_cnt];
			double[] orderLineAmounts = new double[o_ol_cnt];
			double total_amount = 0;
			char[] brandGeneric = new char[o_ol_cnt];
			/*
			 * // retrieve w_tax rate stmt = conn.prepareStatement("SELECT W_TAX " +
			 * "  FROM " + "WAREHOUSE" + " WHERE W_ID = ?"); stmt.setInt(1, w_id); ResultSet
			 * w_rs = stmt.executeQuery(); if (!w_rs.next()) {
			 * System.out.println("ERROR_11: Invalid warehouse id: " + w_id); return 11; }
			 * double w_tax = w_rs.getDouble("W_TAX"); w_rs.close();
			 */ //
				// retrieve d_tax rate and update D_NEXT_O_ID
			stmt = conn.prepareStatement(
					"SELECT D_NEXT_O_ID, D_TAX " + "  FROM " + "DISTRICT" + " WHERE D_W_ID = ? AND D_ID = ?");
			stmt.setInt(1, w_id);
			stmt.setInt(2, d_id);
			ResultSet d_rs = stmt.executeQuery();
			if (!d_rs.next()) {
				System.out.println("ERROR_12: Invalid district id: (" + w_id + "," + d_id + ")");
				return 12;
			}
			int d_next_o_id = d_rs.getInt("D_NEXT_O_ID");
			double d_tax = d_rs.getDouble("D_TAX");

			stmt = conn.prepareStatement(
					"UPDATE " + "DISTRICT" + "   SET D_NEXT_O_ID = ? " + " WHERE D_W_ID = ? " + "   AND D_ID = ?");
			stmt.setInt(1, d_next_o_id + 1);
			stmt.setInt(2, w_id);
			stmt.setInt(3, d_id);
			stmt.executeUpdate();
			int o_id = d_next_o_id;
			//
			/*
			 * // insert a new row into OORDER and NEW_ORDER tables stmt =
			 * conn.prepareStatement( "INSERT INTO " + "OORDER" +
			 * " (O_ID, O_D_ID, O_W_ID, O_C_ID, O_ENTRY_D, O_OL_CNT, O_ALL_LOCAL)" +
			 * " VALUES (?, ?, ?, ?, ?, ?, ?)"); stmt.setInt(1, o_id); stmt.setInt(2, d_id);
			 * stmt.setInt(3, w_id); stmt.setInt(4, c_id); stmt.setTimestamp(5, new
			 * Timestamp(System.currentTimeMillis())); stmt.setInt(6, o_ol_cnt);
			 * stmt.setInt(7, o_all_local); stmt.executeUpdate(); // stmt =
			 * conn.prepareStatement( "INSERT INTO " + "NEW_ORDER" +
			 * " (NO_O_ID, NO_D_ID, NO_W_ID) " + " VALUES ( ?, ?, ?)"); stmt.setInt(1,
			 * o_id); stmt.setInt(2, d_id); stmt.setInt(3, w_id); stmt.executeUpdate();
			 * 
			 * // // retrieve customer's information stmt =
			 * conn.prepareStatement("SELECT C_DISCOUNT, C_LAST, C_CREDIT" + "  FROM " +
			 * "CUSTOMER" + " WHERE C_W_ID = ? " + "   AND C_D_ID = ? " +
			 * "   AND C_ID = ?"); stmt.setInt(1, w_id); stmt.setInt(2, d_id);
			 * stmt.setInt(3, c_id); ResultSet c_rs = stmt.executeQuery(); if (!c_rs.next())
			 * { System.out.println("ERROR_13: Invalid customer id: (" + w_id + "," + d_id +
			 * "," + c_id + ")"); return 13; }
			 * 
			 * double c_discount = c_rs.getDouble("C_DISCOUNT"); String c_last =
			 * c_rs.getString("C_LAST"); String c_credit = c_rs.getString("C_CREDIT");
			 * System.out.println("======="); // For each O_OL_CNT item on the order perform
			 * the following tasks
			 */
			PreparedStatement i_stmt = conn.prepareStatement("INSERT INTO " + "ORDER_LINE"
					+ " (OL_O_ID, OL_D_ID, OL_W_ID, OL_NUMBER, OL_I_ID, OL_SUPPLY_W_ID, OL_QUANTITY, OL_AMOUNT, OL_DIST_INFO) "
					+ " VALUES (?,?,?,?,?,?,?,?,?)");

			Statement statement = conn.createStatement();
			for (int ol_number = 1; ol_number <= o_ol_cnt; ol_number++) {
				int ol_supply_w_id = supplierWarehouseIDs[ol_number - 1];
				int ol_i_id = itemIDs[ol_number - 1];
				int ol_quantity = orderQuantities[ol_number - 1];
				// retrieve item
				/*
				 * stmt = conn .prepareStatement("SELECT I_PRICE, I_NAME , I_DATA " + "  FROM "
				 * + "ITEM" + " WHERE I_ID = ?"); stmt.setInt(1, ol_i_id); ResultSet i_rs =
				 * stmt.executeQuery(); // this is expected to happen 1% of the times if
				 * (!i_rs.next()) { if (ol_number != o_ol_cnt) {
				 * System.out.println("ERROR_14: Invalid item id: (" + ol_i_id +
				 * ") given in the middle of the order list (unexpected)"); return 14; }
				 * System.out.println("EXPECTED_ERROR_15: Invalid item id: (" + ol_i_id + ")");
				 * return 15; }
				 */
				double i_price = 5;
				// String i_name = i_rs.getString("I_NAME");
				// String i_data = i_rs.getString("I_DATA");
				// i_rs.close();

				itemPrices[ol_number - 1] = i_price;
				// itemNames[ol_number - 1] = i_name;

				// retrieve stock
				stmt = conn.prepareStatement("SELECT  *  FROM " + "STOCK" + " WHERE S_I_ID = ? " + "   AND S_W_ID = ?");
				stmt.setInt(1, ol_i_id);
				stmt.setInt(2, ol_supply_w_id);
				ResultSet s_rs = stmt.executeQuery();
				if (!s_rs.next()) {
					System.out.println("ERROR_16: Invalid stock primary key: (" + ol_i_id + "," + ol_supply_w_id + ")");
					return 16;
				}
				double s_quantity = s_rs.getDouble("S_QUANTITY");
				double s_ytd = s_rs.getDouble("S_YTD");
				int s_order_cnt = s_rs.getInt("S_ORDER_CNT");
				int s_remote_cnt = s_rs.getInt("S_REMOTE_CNT");
				// String s_data = s_rs.getString("S_DATA");
				// String s_dist_01 = s_rs.getString("S_DIST_01");
				// String s_dist_02 = s_rs.getString("S_DIST_02");
				// String s_dist_03 = s_rs.getString("S_DIST_03");
				// String s_dist_04 = s_rs.getString("S_DIST_04");
				// String s_dist_05 = s_rs.getString("S_DIST_05");
				// String s_dist_06 = s_rs.getString("S_DIST_06");
				// String s_dist_07 = s_rs.getString("S_DIST_07");
				// String s_dist_08 = s_rs.getString("S_DIST_08");
				// String s_dist_09 = s_rs.getString("S_DIST_09");
				// String s_dist_10 = s_rs.getString("S_DIST_10");
				// s_rs.close();
				//
				// stockQuantities[ol_number - 1] = s_quantity;
				// if (s_quantity - ol_quantity >= 10) {
				// s_quantity -= ol_quantity; // new s_quantity
				// } else {
				// s_quantity += -ol_quantity + 91; // new s_quantity
				// }
				// int s_remote_cnt_increment;
				// if (ol_supply_w_id == w_id) {
				// s_remote_cnt_increment = 0;
				// } else {
				// s_remote_cnt_increment = 1;
				// }
				// update stock row
				stmtUpdateStock = conn.prepareStatement("UPDATE " + "STOCK" + " SET S_QUANTITY = ?," + "S_YTD = ?,"
						+ "S_ORDER_CNT = ?," + "S_REMOTE_CNT = ? " + " WHERE S_I_ID = ? " + "   AND S_W_ID = ?");
				stmtUpdateStock.setDouble(1, 69);
				stmtUpdateStock.setDouble(2, s_ytd + ol_quantity);
				stmtUpdateStock.setInt(3, s_order_cnt + 1);
				stmtUpdateStock.setInt(4, s_remote_cnt + 69);
				stmtUpdateStock.setInt(5, ol_i_id);
				stmtUpdateStock.setInt(6, ol_supply_w_id);
				stmtUpdateStock.executeUpdate();
				System.out.println("d_id: " + d_id + "   o_id: " + o_id + "   ol_i_id: " + ol_i_id + "   ol_quantity: "
						+ ol_quantity + "    s_ytd: " + s_ytd + "	ol_number:" + ol_number);
				//
				double ol_amount = ol_quantity * i_price;
				/*
				 * orderLineAmounts[ol_number - 1] = ol_amount; total_amount += ol_amount; if
				 * (i_data.indexOf("ORIGINAL") != -1 && s_data.indexOf("ORIGINAL") != -1) {
				 * brandGeneric[ol_number - 1] = 'B'; } else { brandGeneric[ol_number - 1] =
				 * 'G'; } String ol_dist_info = null; switch ((int) d_id) { case 1: ol_dist_info
				 * = s_dist_01; break; case 2: ol_dist_info = s_dist_02; break; case 3:
				 * ol_dist_info = s_dist_03; break; case 4: ol_dist_info = s_dist_04; break;
				 * case 5: ol_dist_info = s_dist_05; break; case 6: ol_dist_info = s_dist_06;
				 * break; case 7: ol_dist_info = s_dist_07; break; case 8: ol_dist_info =
				 * s_dist_08; break; case 9: ol_dist_info = s_dist_09; break; case 10:
				 * ol_dist_info = s_dist_10; break; }
				 */
				//
				// insert a row into orderline table representing each order item

				statement.addBatch("INSERT INTO " + "ORDER_LINE"
						+ " (OL_O_ID, OL_D_ID, OL_W_ID, OL_NUMBER, OL_I_ID, OL_SUPPLY_W_ID, OL_QUANTITY, OL_AMOUNT, OL_DIST_INFO) "
						+ " VALUES (" + o_id + "," + d_id + "," + w_id + "," + ol_number + "," + ol_i_id + ","
						+ ol_supply_w_id + "," + ol_quantity + "," + ol_amount + "," + "\'salam\'" + ")");
				/*
				 * 
				 * i_stmt.setInt(1, o_id); i_stmt.setInt(2, d_id); i_stmt.setInt(3, w_id);
				 * i_stmt.setInt(4, ol_number); i_stmt.setInt(5, ol_i_id); i_stmt.setInt(6,
				 * ol_supply_w_id); i_stmt.setDouble(7, ol_quantity); i_stmt.setDouble(8,
				 * ol_amount); i_stmt.setString(9, "INFO: "+String.valueOf(ol_i_id));
				 * i_stmt.addBatch();
				 */

			}
			int[] counts = statement.executeBatch();
			statement.close();
			System.out.println("=======" + o_id + "," + d_id);
			for (int kir : counts)
				System.out.print(kir + ",");
			System.out.println();
			System.out.println("=======");
			// i_stmt.executeBatch();
			// stmtUpdateStock.executeBatch();
			// total_amount *= (1 + w_tax + d_tax) * (1 - c_discount);
			// stmt.clearBatch();
			stmt.close();
			//
			// ❄❄❄❄❄❄❄❄❄❄❄❄❄❄❄
			// TXN SUCCESSFUL!
			// ❄❄❄❄❄❄❄❄❄❄❄❄❄❄❄
			if (_VERBOSE)
				System.out.println("SUCCESS!");
			return 0;
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("\n\n\n\n");
			System.out.println("stmt--->" + stmt);
			System.out.println("\n\n\n\n");
			return -1;
		}
	}
}
//
//
//
//
//
//
