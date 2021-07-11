package seats;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UpdateCustomer {
	public static int updateCustomer(Connection connect, long c_id, String c_id_str, long update_ff, long attr0,
			long attr1) throws Exception {
		try {
			if (c_id == -1) {
				PreparedStatement stmt1 = connect.prepareStatement("SELECT C_ID FROM CUSTOMER WHERE C_ID_STR = ? ");
				stmt1.setString(1, c_id_str);
				ResultSet rs1 = stmt1.executeQuery();
				if (rs1.next()) {
					c_id = rs1.getLong("C_ID");
					rs1.close();
				} else {
					rs1.close();
					System.out.println(
							(String.format("ERROR_51 : No Customer information record found for string:" + c_id_str)));
					return (Constants._NO_ERROR_MODE) ? 0 : 51;
				}
			}
			PreparedStatement stmt2 = connect.prepareStatement("SELECT * FROM CUSTOMER WHERE C_ID = ? ");
			stmt2.setLong(1, c_id);
			ResultSet rs2 = stmt2.executeQuery();
			if (rs2.next() == false) {
				rs2.close();
				System.out.println(String.format("ERROR_52: No Customer information record found for id: %d", c_id));
				return (Constants._NO_ERROR_MODE) ? 0 : 52;
			}
			if (c_id != rs2.getInt(1)) {
				System.out.println(String.format("ERROR_53: impossible state: wrong customer is retrieved"));
				return (Constants._NO_ERROR_MODE) ? 0 : 53;
			}

			int base_airport = rs2.getInt("C_BASE_AP_ID");
			rs2.close();

			// Get their airport information
			PreparedStatement stmt31 = connect.prepareStatement("SELECT * " + "  FROM AIRPORT WHERE AP_ID = ?");
			stmt31.setInt(1, base_airport);
			ResultSet airport_results = stmt31.executeQuery();
			boolean adv = airport_results.next();
			if (!adv) {
				System.out.println("ERROR_54: base airport_id is invalid");
				return (Constants._NO_ERROR_MODE) ? 0 : 54;
			}

			PreparedStatement stmt32 = connect.prepareStatement("SELECT * " + "  FROM COUNTRY WHERE CO_ID = ?");
			stmt32.setLong(1, airport_results.getInt("AP_CO_ID"));
			ResultSet country_results = stmt32.executeQuery();
			adv = country_results.next() && adv;
			airport_results.close();
			if (!adv) {
				System.out.println("ERROR_55: country does not exist");
				return (Constants._NO_ERROR_MODE) ? 0 : 55;
			}

			if (update_ff != -1) {
				PreparedStatement stmt4 = connect.prepareStatement("SELECT * FROM FREQUENT_FLYER WHERE FF_C_ID = ?");
				stmt4.setLong(1, c_id);
				ResultSet ff_results = stmt4.executeQuery();

				while (ff_results.next()) {
					int ff_al_id = ff_results.getInt("FF_AL_ID");
					PreparedStatement stmt5 = connect.prepareStatement(
							"UPDATE FREQUENT_FLYER SET FF_IATTR00 = ?, FF_IATTR01 = ?  WHERE FF_C_ID = ? AND FF_AL_ID = ? ");
					stmt5.setLong(1, attr0);
					stmt5.setLong(2, attr1);
					stmt5.setLong(3, c_id);
					stmt5.setLong(4, ff_al_id);
					stmt5.executeUpdate();
				} // WHILE
				ff_results.close();

				PreparedStatement stmt6 = connect.prepareStatement(
						"UPDATE CUSTOMER SET C_IATTR00 = ?, C_IATTR01 = ? WHERE C_ID = ? AND C_ID_STR = ?");
				stmt6.setLong(1, attr0);
				stmt6.setLong(2, attr1);
				stmt6.setLong(3, c_id);
				stmt6.setString(4, String.valueOf(c_id));
				stmt6.executeUpdate();
			}

			// ❄❄❄❄❄❄❄❄❄❄❄❄❄❄❄
			// TXN SUCCESSFUL!
			// ❄❄❄❄❄❄❄❄❄❄❄❄❄❄❄
			return 0;
		} catch (Exception e) {
			throw e;
		} finally {

		}

	}

}
