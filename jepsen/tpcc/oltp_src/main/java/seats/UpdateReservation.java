package seats;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UpdateReservation {

	public static int updateReservation(Connection connect, long r_id, long f_id, long c_id, long seatnum,
			long attr_idx, long attr_val) throws Exception {
		try {
			PreparedStatement stmt1 = connect.prepareStatement(
					("SELECT R_ID " + "  FROM RESERVATION WHERE R_F_ID = ? and R_SEAT = ? ALLOW FILTERING"));
			stmt1.setLong(1, f_id);
			stmt1.setLong(2, seatnum);
			ResultSet results1 = stmt1.executeQuery();
			boolean found1 = results1.next();
			results1.close();
			if (found1) {
				System.out.println(String.format("ERROR_61: Seat %d is already reserved on flight %d", seatnum, f_id));
				return (Constants._NO_ERROR_MODE) ? 0 : 61;
			}

			PreparedStatement stmt2 = connect.prepareStatement(
					"SELECT R_ID " + "  FROM RESERVATION WHERE R_F_ID = ? AND R_C_ID = ?  ALLOW FILTERING");
			stmt2.setLong(1, f_id);
			stmt2.setLong(2, c_id);
			ResultSet results2 = stmt2.executeQuery();
			boolean found2 = results2.next();
			results2.close();
			if (!found2) {
				System.out.println(String.format(
						"ERROR_62: Customer %d does not have an existing reservation on flight #%d", c_id, f_id));
				return (Constants._NO_ERROR_MODE) ? 0 : 62;
			}

			if (!found1 && found2) { // minor simplification compared to original SEATS
				String BASE_SQL = "UPDATE RESERVATION SET R_SEAT = ?, R_IATTR00 = ? "
						+ " WHERE R_ID = ? AND R_C_ID = ? AND R_F_ID = ?";
				PreparedStatement stmt3 = connect.prepareStatement(BASE_SQL);
				stmt3.setLong(1, seatnum);
				stmt3.setLong(2, attr_val);
				stmt3.setLong(3, r_id);
				stmt3.setLong(4, c_id);
				stmt3.setLong(5, f_id);
				stmt3.executeUpdate();
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
