package tpcc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import java.sql.Connection;

public class Payment {
	@SuppressWarnings("resource")
	public static int payment(Connection conn, int w_id, int d_id, boolean customerByName, int c_id,
			String c_last, int customerWarehouseID, int customerDistrictID, double paymentAmount) throws Exception {
		PreparedStatement stmt = null;
		try {
			boolean isRemote = (w_id != customerDistrictID);
			double w_ydt, d_ytd;
			String w_street_1, w_street_2, w_city, w_state, w_zip, w_name;
			String d_street_1, d_street_2, d_city, d_state, d_zip, d_name;
			// read necessary columns from warehouse
			stmt = conn.prepareStatement("SELECT W_YTD, W_STREET_1, W_STREET_2, W_CITY, W_STATE, W_ZIP, W_NAME"
					+ "  FROM " + "WAREHOUSE" + " WHERE W_ID = ?");
			stmt.setInt(1, w_id);
			ResultSet w_rs = stmt.executeQuery();
			if (!w_rs.next()) {
				System.out.println("ERROR_21: Invalid warehouse id: " + w_id);
				return 21;
			}
			w_ydt = w_rs.getDouble("W_YTD");
			w_street_1 = w_rs.getString("W_STREET_1");
			w_street_2 = w_rs.getString("W_STREET_2");
			w_city = w_rs.getString("W_CITY");
			w_state = w_rs.getString("W_STATE");
			w_zip = w_rs.getString("W_ZIP");
			w_name = w_rs.getString("W_NAME");
			w_rs.close();
			//
			// update W_YTD by paymentAmount
			stmt = conn.prepareStatement("UPDATE " + "WAREHOUSE" + "   SET W_YTD = ? " + " WHERE W_ID = ? ");
			stmt.setDouble(1, w_ydt + paymentAmount);
			stmt.setInt(2, w_id);
			stmt.executeUpdate();

			//
			// read necessary columns from district
			stmt = conn.prepareStatement("SELECT D_YTD, D_STREET_1, D_STREET_2, D_CITY, D_STATE, D_ZIP, D_NAME"
					+ "  FROM " + "DISTRICT" + " WHERE D_W_ID = ? " + "   AND D_ID = ?");
			stmt.setInt(1, w_id);
			stmt.setInt(2, d_id);
			ResultSet d_rs = stmt.executeQuery();
			if (!d_rs.next()) {
				System.out.println("ERROR_22: Invalid district id: " + w_id + "," + d_id);
				return 22;
			}
			d_ytd = d_rs.getDouble("D_YTD");
			d_street_1 = d_rs.getString("D_STREET_1");
			d_street_2 = d_rs.getString("D_STREET_2");
			d_city = d_rs.getString("D_CITY");
			d_state = d_rs.getString("D_STATE");
			d_zip = d_rs.getString("D_ZIP");
			d_name = d_rs.getString("D_NAME");
			d_rs.close();
			//
			// update D_YTD by paymentAmount
			stmt = conn
					.prepareStatement("UPDATE " + "DISTRICT" + "   SET D_YTD = ? " + " WHERE D_W_ID = ? AND D_ID = ? ");
			stmt.setDouble(1, d_ytd + paymentAmount);
			stmt.setInt(2, w_id);
			stmt.setInt(3, d_id);
			stmt.executeUpdate();

			//
			// Retrieve customer's information

			String c_first, c_middle, c_street_1, c_street_2, c_city, c_state, c_zip, c_phone, c_credit;
			double c_credit_lim, c_discount, c_balance;
			float c_ytd_payment;
			int c_payment_cnt;
			Timestamp c_since;
			if (customerByName) {
				stmt = conn.prepareStatement("SELECT C_ID" + "  FROM " + "CUSTOMER" + " WHERE C_W_ID = ? "
						+ "   AND C_D_ID = ? " + "   AND C_LAST = ? " + "");
				stmt.setInt(1, customerWarehouseID);
				stmt.setInt(2, customerDistrictID);
				stmt.setString(3, c_last);
				ResultSet c_rs = stmt.executeQuery();
				// find the appropriate index
				int index = 0;
				List<Integer> all_c_ids = new ArrayList<Integer>();
				while (c_rs.next()) {
					index++;
					all_c_ids.add(c_rs.getInt("C_ID"));
				}
				if (index == 0) {
					System.out.println("ERROR_23: No customer with the given last name: " + customerWarehouseID + ","
							+ customerDistrictID + "," + c_last);
					return 23;
				}
				if (index % 2 != 0)
					index++;
				index = (index / 2);
				c_id = all_c_ids.get(index - 1);

				stmt = conn.prepareStatement("SELECT C_FIRST, C_MIDDLE, C_LAST, C_STREET_1, C_STREET_2,"
						+ "C_CITY, C_STATE, C_ZIP, C_PHONE, C_CREDIT, C_CREDIT_LIM,"
						+ "   C_DISCOUNT, C_BALANCE, C_YTD_PAYMENT, C_PAYMENT_CNT, C_SINCE " + "  FROM " + "CUSTOMER"
						+ " WHERE C_W_ID = ? " + "   AND C_D_ID = ? " + "   AND C_ID = ? ");
				stmt.setInt(1, customerWarehouseID);
				stmt.setInt(2, customerDistrictID);
				stmt.setInt(3, c_id);
				c_rs = stmt.executeQuery();

				c_first = c_rs.getString("c_first");
				c_middle = c_rs.getString("c_middle");
				c_street_1 = c_rs.getString("c_street_1");
				c_street_2 = c_rs.getString("c_street_2");
				c_city = c_rs.getString("c_city");
				c_state = c_rs.getString("c_state");
				c_zip = c_rs.getString("c_zip");
				c_phone = c_rs.getString("c_phone");
				c_credit = c_rs.getString("c_credit");
				c_credit_lim = c_rs.getDouble("c_credit_lim");
				c_discount = c_rs.getDouble("c_discount");
				c_balance = c_rs.getDouble("c_balance");
				c_ytd_payment = c_rs.getFloat("c_ytd_payment");
				c_payment_cnt = c_rs.getInt("c_payment_cnt");
				c_since = c_rs.getTimestamp("c_since");
				c_rs.close();

			} else {
				// retrieve customer by id
				stmt = conn.prepareStatement("SELECT C_FIRST, C_MIDDLE, C_LAST, C_STREET_1, C_STREET_2, "
						+ "       C_CITY, C_STATE, C_ZIP, C_PHONE, C_CREDIT, C_CREDIT_LIM, "
						+ "       C_DISCOUNT, C_BALANCE, C_YTD_PAYMENT, C_PAYMENT_CNT, C_SINCE " + "  FROM "
						+ "CUSTOMER" + " WHERE C_W_ID = ? " + "   AND C_D_ID = ? " + "   AND C_ID = ?");
				stmt.setInt(1, customerWarehouseID);
				stmt.setInt(2, customerDistrictID);
				stmt.setInt(3, c_id);
				ResultSet c_rs = stmt.executeQuery();
				if (!c_rs.next()) {
					System.out.println("ERROR_24: Invalid customer id: " + customerWarehouseID + ","
							+ customerDistrictID + "," + c_id);
					return 24;
				}
				c_first = c_rs.getString("c_first");
				c_middle = c_rs.getString("c_middle");
				c_street_1 = c_rs.getString("c_street_1");
				c_street_2 = c_rs.getString("c_street_2");
				c_city = c_rs.getString("c_city");
				c_state = c_rs.getString("c_state");
				c_zip = c_rs.getString("c_zip");
				c_phone = c_rs.getString("c_phone");
				c_credit = c_rs.getString("c_credit");
				c_credit_lim = c_rs.getDouble("c_credit_lim");
				c_discount = c_rs.getDouble("c_discount");
				c_balance = c_rs.getDouble("c_balance");
				c_ytd_payment = c_rs.getFloat("c_ytd_payment");
				c_payment_cnt = c_rs.getInt("c_payment_cnt");
				c_since = c_rs.getTimestamp("c_since");
				c_rs.close();
			}
			//
			// Update customers info
			c_balance -= paymentAmount;
			c_ytd_payment += paymentAmount;
			c_payment_cnt++;
			String c_data = null;
			if (c_credit.equals("BC")) {
				// bad credit (c_data is also updated)
				stmt = conn.prepareStatement("SELECT C_DATA " + "  FROM " + "CUSTOMER" + " WHERE C_W_ID = ? "
						+ "   AND C_D_ID = ? " + "   AND C_ID = ?");
				stmt.setInt(1, customerWarehouseID);
				stmt.setInt(2, customerDistrictID);
				stmt.setInt(3, c_id);
				ResultSet c_rs = stmt.executeQuery();
				if (!c_rs.next()) {
					System.out.println("ERROR_25: Invalid customer id: " + customerWarehouseID + ","
							+ customerDistrictID + "," + c_id);
					return 25;
				}
				c_data = c_rs.getString("C_DATA");
				c_rs.close();
				c_data = c_id + " " + customerDistrictID + " " + customerWarehouseID + " " + d_id + " " + w_id + " "
						+ paymentAmount + " | " + c_data;
				if (c_data.length() > 500)
					c_data = c_data.substring(0, 500);
				stmt = conn.prepareStatement("UPDATE " + "CUSTOMER" + "   SET C_BALANCE = ?, "
						+ "       C_YTD_PAYMENT = ?, " + "       C_PAYMENT_CNT = ?, " + "       C_DATA = ? "
						+ " WHERE C_W_ID = ? "
						+ "   AND C_D_ID = ? " + "   AND C_ID = ?");
				stmt.setDouble(1, c_balance);
				stmt.setFloat(2, c_ytd_payment);
				stmt.setInt(3, c_payment_cnt);
				stmt.setString(4, c_data);
				stmt.setInt(5, customerWarehouseID);
				stmt.setInt(6, customerDistrictID);
				stmt.setInt(7, c_id);
				stmt.executeUpdate();
			} else {
				// good credit (no need to update c_data)
				stmt = conn.prepareStatement("UPDATE " + "CUSTOMER" + "   SET C_BALANCE = ?, "
						+ "       C_YTD_PAYMENT = ?, " + "       C_PAYMENT_CNT = ? " + " WHERE C_W_ID = ? "
						+ "   AND C_D_ID = ? " + "   AND C_ID = ?");
				stmt.setDouble(1, c_balance);
				stmt.setDouble(2, c_ytd_payment);
				stmt.setInt(3, c_payment_cnt);
				stmt.setInt(4, customerWarehouseID);
				stmt.setInt(5, customerDistrictID);
				stmt.setInt(6, c_id);
				stmt.executeUpdate();
			}

			// create H_DATA and insert a new row into HISTORY
			if (w_name.length() > 10)
				w_name = w_name.substring(0, 10);
			if (d_name.length() > 10)
				d_name = d_name.substring(0, 10);
			String h_data = w_name + "    " + d_name;
			stmt = conn.prepareStatement("SELECT H_AMOUNT FROM HISTORY WHERE" + " H_C_D_ID=?" + " AND H_C_W_ID=?"
					+ " AND H_C_ID=?" + " AND H_D_ID=?" + " AND H_W_ID=?");
			stmt.setInt(1, customerDistrictID);
			stmt.setInt(2, customerWarehouseID);
			stmt.setInt(3, c_id);
			stmt.setInt(4, d_id);
			stmt.setInt(5, w_id);
			ResultSet h_rs = stmt.executeQuery();
			double old_amount = 0;
			if (h_rs.next())
				old_amount += h_rs.getDouble("H_AMOUNT");
			stmt = conn.prepareStatement("INSERT INTO " + "HISTORY"
					+ " (H_C_D_ID, H_C_W_ID, H_C_ID, H_D_ID, H_W_ID, H_DATE, H_AMOUNT, H_DATA) "
					+ " VALUES (?,?,?,?,?,?,?,?)");
			stmt.setInt(1, customerDistrictID);
			stmt.setInt(2, customerWarehouseID);
			stmt.setInt(3, c_id);
			stmt.setInt(4, d_id);
			stmt.setInt(5, w_id);
			stmt.setTimestamp(6, new Timestamp(System.currentTimeMillis()));
			stmt.setDouble(7, old_amount + paymentAmount);
			stmt.setString(8, h_data);
			stmt.executeUpdate();

			//
			//
			//
			//
			//
			//
			//
			// ❄❄❄❄❄❄❄❄❄❄❄❄❄❄❄
			// TXN SUCCESSFUL!
			// ❄❄❄❄❄❄❄❄❄❄❄❄❄❄❄
			stmt.close();
			return 0;
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
	}
}
