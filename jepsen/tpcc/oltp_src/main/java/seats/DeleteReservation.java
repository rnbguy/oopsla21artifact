package seats;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Connection;

 
public class DeleteReservation {
	public static int deleteReservation(Connection conn, long f_id, Long c_id, String c_id_str,
			String ff_c_id_str, Long ff_al_id) throws Exception {
		try {
			PreparedStatement stmt = null;
			// If we weren't given the customer id, then look it up
			if (c_id == -1) {
				// Use the customer's id as a string
				assert (c_id_str != null && c_id_str.length() > 0);
				stmt = conn.prepareStatement("SELECT C_ID FROM CUSTOMER WHERE C_ID_STR = ? ");
				stmt.setString(1, c_id_str);
				ResultSet results = stmt.executeQuery();
				if (results.next()) {
					c_id = results.getLong("C_ID");
				} else {
					results.close();
					System.out.println(String.format(
							"ERROR_11: No Customer record was found [c_id_str=%s, ff_c_id_str=%s, ff_al_id=%s]",
							c_id_str, ff_c_id_str, ff_al_id));
					return (Constants._NO_ERROR_MODE) ? 0 : 11;
				}
				results.close();
			}

			// We are chopping the original query with joins on three table into
			// three separate queries. We also read extra columns which will be used later
			// when updating them

			// 1
			stmt = conn.prepareStatement(
					"SELECT C_SATTR00, C_SATTR02, C_SATTR04, C_IATTR00, C_IATTR02, C_IATTR04, C_IATTR06, C_BALANCE, C_IATTR10, C_IATTR11 FROM CUSTOMER WHERE C_ID = ?");
			stmt.setLong(1, c_id);
			ResultSet results2 = stmt.executeQuery();
			if (results2.next() == false) {
				results2.close();
				System.out.println("ERROR_12: c_id " + c_id + " does not exist");
				return (Constants._NO_ERROR_MODE) ? 0 : 12;
			}

			float oldBal = results2.getFloat("C_BALANCE");
			long oldAttr10 = results2.getLong("C_IATTR10");
			long oldAttr11 = results2.getLong("C_IATTR11");
			String c_iattr00 = results2.getString("C_SATTR00");

			// 2
			stmt = conn.prepareStatement("SELECT F_SEATS_LEFT FROM FLIGHT WHERE F_ID = ? ");
			stmt.setLong(1, f_id);
			ResultSet results3 = stmt.executeQuery();
			boolean flight_exists = results3.next();
			if (!flight_exists) {
				results3.close();
				System.out.println("ERROR_13: f_id " + f_id + " does not exist");
				return (Constants._NO_ERROR_MODE) ? 0 : 13;
			}
			int seats_left = results3.getInt("F_SEATS_LEFT");

			// 3
			stmt = conn.prepareStatement(
					"SELECT R_ID, R_SEAT, R_PRICE, R_IATTR00 FROM RESERVATION WHERE R_C_ID = ? AND R_F_ID = ? ALLOW FILTERING");
			stmt.setLong(1, c_id);
			stmt.setLong(2, f_id);
			ResultSet results4 = stmt.executeQuery();
			boolean reservation_exists = results4.next();
			if (!reservation_exists) {
				System.out.println("ERROR_14: reservation does not exist:" + "r_f_id:" + f_id + "    r_c_id:" + c_id);
				return (Constants._NO_ERROR_MODE) ? 0 : 14;
			}

			int r_id = results4.getInt("R_ID");
			float r_price = results4.getFloat("R_PRICE");
			results4.close();
			int updated = 0;

			// Now delete all of the flights that they have on this flight
			stmt = conn.prepareStatement("DELETE FROM RESERVATION WHERE R_ID = ? AND R_C_ID = ? AND R_F_ID = ?");
			stmt.setLong(1, r_id);
			stmt.setLong(2, c_id);
			stmt.setLong(3, f_id);
			updated = stmt.executeUpdate();
			if (updated != 0) {
				System.out.println(String.format("ERROR_15: delete did NOT succeed: r_id: %d   c_id: %d    f_id: %d",
						r_id, c_id, f_id));
				return (Constants._NO_ERROR_MODE) ? 0 : 15;
			}

			// Update Available Seats on Flight
			stmt = conn.prepareStatement("UPDATE FLIGHT SET F_SEATS_LEFT = ?" + " WHERE F_ID = ? ");
			stmt.setLong(1, seats_left + 1);
			stmt.setLong(2, f_id);
			updated = stmt.executeUpdate();
			if (updated != 0) {
				System.out.println(String.format("ERROR_16: update flight did NOT succeed: f_id: %d", f_id));
				return (Constants._NO_ERROR_MODE) ? 0 : 16;
			}

			// Update Customer's Balance
			stmt = conn.prepareStatement(
					"UPDATE CUSTOMER SET C_BALANCE = ?, C_IATTR00 = ?, C_IATTR10 = ?,  C_IATTR11 = ? WHERE C_ID = ? AND C_ID_STR = ?");
			stmt.setFloat(1, oldBal + (r_price));
			stmt.setString(2, c_iattr00);
			stmt.setLong(3, oldAttr10 - 1);
			stmt.setLong(4, oldAttr11 - 1);
			stmt.setLong(5, c_id);
			stmt.setString(6, String.valueOf(c_id));
			updated = stmt.executeUpdate();
			if (updated != 0) {
				System.out.println(String.format("ERROR_17: update customer balance did NOT succeed: c_id: %d", c_id));
				return (Constants._NO_ERROR_MODE) ? 0 : 17;
			}

			// Update Customer's Frequent Flyer Information (Optional)
			if (ff_al_id != -1) {
				stmt = conn.prepareStatement(
						"SELECT FF_IATTR10 FROM FREQUENT_FLYER " + " WHERE FF_C_ID = ? " + "   AND FF_AL_ID = ?");
				stmt.setLong(1, c_id);
				stmt.setLong(2, ff_al_id);
				ResultSet results5 = stmt.executeQuery();
				boolean ff_exists = results5.next();
				if (!ff_exists) {
					System.out.println(String.format("ERROR_18: Frequent Flyer does NOT exist: c_id: %d   ff_al_id: %d",
							c_id, ff_al_id));
					return (Constants._NO_ERROR_MODE) ? 0 : 18;
				}
				long olAttr10 = results5.getLong("FF_IATTR10");
				stmt = conn.prepareStatement(
						"UPDATE FREQUENT_FLYER SET FF_IATTR10 = ?" + " WHERE FF_C_ID = ? " + "   AND FF_AL_ID = ?");
				stmt.setLong(1, olAttr10 - 1);
				stmt.setLong(2, c_id);
				stmt.setLong(3, ff_al_id);
				updated = stmt.executeUpdate();
				if (updated != 0) {
					System.out.println(String.format(
							"ERROR_19: Failed to update FrequentFlyer info [c_id=%d, ff_al_id=%d]", c_id, ff_al_id));
					return (Constants._NO_ERROR_MODE) ? 0 : 19;
				}
			}
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
