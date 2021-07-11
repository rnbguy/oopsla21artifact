package seats;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class NewReservation {

	public static int newReservation(Connection connect, long r_id, long c_id, long f_id, int seatnum, float price,
			long attrs[]) throws Exception {
		try {
			// Flight Information
			PreparedStatement stmt11 = connect
					.prepareStatement("SELECT F_AL_ID, F_SEATS_LEFT FROM FLIGHT WHERE F_ID = ?");
			stmt11.setLong(1, f_id);
			ResultSet rs1 = stmt11.executeQuery();
			boolean found1 = rs1.next();
			if (!found1) {
				System.out.println("ERROR_41: Invalid F_ID:" + f_id);
				return (Constants._NO_ERROR_MODE) ? 0 : 41;
			}
			long airline_id = rs1.getLong("F_AL_ID");
			long seats_left = rs1.getLong("F_SEATS_LEFT");

			// Airline Information
			PreparedStatement stmt12 = connect.prepareStatement("SELECT * FROM AIRLINE WHERE AL_ID = ?");
			stmt12.setLong(1, airline_id);
			ResultSet rs2 = stmt12.executeQuery();
			boolean found2 = rs2.next();
			if (!found2) {
				System.out.println("ERROR_42: Invalid Airline:" + airline_id);
				return (Constants._NO_ERROR_MODE) ? 0 : 42;
			}
			rs1.close();
			rs2.close();
			if (seats_left <= 0) {
				System.out.println("ERROR_43: No more seats available for flight:" + f_id);
				return (Constants._NO_ERROR_MODE) ? 0 : 43;
			} // Check if Seat is Available
			PreparedStatement stmt2 = connect
					.prepareStatement("SELECT R_ID FROM RESERVATION WHERE R_F_ID = ? and R_SEAT = ? ALLOW FILTERING");
			stmt2.setLong(1, f_id);
			stmt2.setLong(2, seatnum);
			ResultSet rs3 = stmt2.executeQuery();
			boolean found3 = rs3.next();
			if (found3) {
				System.out
						.println(String.format(" ERROR_44: Seat %d is already reserved on flight #%d", seatnum, f_id));
				return (Constants._NO_ERROR_MODE) ? 0 : 44;
			}

			// Check if the Customer already has a seat on this flight
			PreparedStatement stmt3 = connect.prepareStatement(
					"SELECT R_ID " + "  FROM RESERVATION WHERE R_F_ID = ? AND R_C_ID = ? ALLOW FILTERING");
			stmt3.setLong(1, f_id);
			stmt3.setLong(2, c_id);
			ResultSet rs4 = stmt3.executeQuery();
			boolean found4 = rs4.next();
			if (found4) {
				System.out.println(String.format("ERROR_45: Customer %d already owns on a reservations on flight #%d",
						c_id, f_id));
				return (Constants._NO_ERROR_MODE) ? 0 : 45;
			}

			// Get Customer Information PreparedStatement stmt4 =
			PreparedStatement stmt4 = connect.prepareStatement(
					"SELECT C_BASE_AP_ID, C_BALANCE, C_SATTR00, C_IATTR10, C_IATTR11 FROM CUSTOMER WHERE C_ID = ? ");
			stmt4.setLong(1, c_id);
			ResultSet rs5 = stmt4.executeQuery();
			boolean found5 = rs5.next();
			if (!found5) {
				System.out.println(String.format("ERROR_46: Invalid customer id: %d ", c_id));
				return (Constants._NO_ERROR_MODE) ? 0 : 46;
			}
			int oldAttr10 = rs5.getInt("C_IATTR10");
			int oldAttr11 = rs5.getInt("C_IATTR11");

			PreparedStatement stmt5 = connect.prepareStatement(
					"INSERT INTO RESERVATION (R_ID, R_C_ID, R_F_ID, R_SEAT, R_PRICE, R_IATTR00, R_IATTR01, "
							+ "   R_IATTR02, R_IATTR03, R_IATTR04, R_IATTR05, R_IATTR06, R_IATTR07, R_IATTR08) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
			stmt5.setLong(1, r_id);
			stmt5.setLong(2, c_id);
			stmt5.setLong(3, f_id);
			stmt5.setLong(4, seatnum);
			stmt5.setFloat(5, 6969F);
			for (int i = 0; i < 9; i++)
				stmt5.setLong(6 + i, attrs[i]);
			stmt5.executeUpdate();

			PreparedStatement stmt6 = connect
					.prepareStatement("UPDATE FLIGHT SET F_SEATS_LEFT = ? " + " WHERE F_ID = ? ");
			stmt6.setLong(1, seats_left - 1);
			stmt6.setLong(2, f_id);
			stmt6.executeUpdate();

			// update customer
			PreparedStatement stmt7 = connect.prepareStatement(
					"UPDATE CUSTOMER SET C_IATTR10 = ?, C_IATTR11 = ?, C_IATTR12 = ?, C_IATTR13 = ?, C_IATTR14 = ?, C_IATTR15 = ?"
							+ "  WHERE C_ID = ? AND C_ID_STR = ?");
			stmt7.setLong(1, oldAttr10 + 1);
			stmt7.setLong(2, oldAttr11 + 1);
			stmt7.setLong(3, attrs[0]);
			stmt7.setLong(4, attrs[1]);
			stmt7.setLong(5, attrs[2]);
			stmt7.setLong(6, attrs[3]);
			stmt7.setLong(7, c_id);
			stmt7.setString(8, String.valueOf(c_id));
			stmt7.executeUpdate();
			// update frequent flyer
			PreparedStatement stmt81 = connect
					.prepareStatement("SELECT FF_IATTR10 FROM FREQUENT_FLYER WHERE FF_C_ID = ? AND FF_AL_ID = ?");
			stmt81.setLong(1, c_id);
			stmt81.setLong(2, airline_id);
			ResultSet rs6 = stmt81.executeQuery();
			boolean adv = rs6.next();
			if (!adv) {
				System.out.println("ERROR_47: frequent flyer does not exist");
				return (Constants._NO_ERROR_MODE) ? 0 : 47;
			}
			long oldFFAttr10 = rs6.getLong("FF_IATTR10");

			PreparedStatement stmt82 = connect.prepareStatement(
					"UPDATE FREQUENT_FLYER SET FF_IATTR10 = ?, FF_IATTR11 = ?, FF_IATTR12 = ?, FF_IATTR13 = ?, FF_IATTR14 = ? "
							+ " WHERE FF_C_ID = ? " + "   AND FF_AL_ID = ?");
			stmt82.setLong(1, oldFFAttr10 + 1);
			stmt82.setLong(2, attrs[4]);
			stmt82.setLong(3, attrs[5]);
			stmt82.setLong(4, attrs[6]);
			stmt82.setLong(5, attrs[7]);
			stmt82.setLong(6, c_id);
			stmt82.setLong(7, airline_id);
			stmt82.executeUpdate();
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
