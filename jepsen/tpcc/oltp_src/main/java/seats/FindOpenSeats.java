package seats;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class FindOpenSeats {
	public static int findOpenSeats(Connection connect, long f_id) throws Exception {
		try {

			final long seatmap[] = new long[] { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
					-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
					-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
					-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
					-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
					-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
					-1, -1, -1, -1, -1, -1, -1 };

			PreparedStatement f_stmt = connect.prepareStatement(
					"SELECT F_STATUS, F_BASE_PRICE, F_SEATS_TOTAL, F_SEATS_LEFT FROM FLIGHT WHERE F_ID = ?");
			f_stmt.setLong(1, f_id);
			ResultSet f_results = f_stmt.executeQuery();

			boolean adv = f_results.next();
			if (!adv) {
				System.out.println(String.format("ERROR_31: given f_id (%d) does not exist", f_id));
				return (Constants._NO_ERROR_MODE) ? 0 : 31;
			}

			float base_price = f_results.getFloat("F_BASE_PRICE");
			long seats_left = f_results.getLong("F_SEATS_LEFT");
			long seats_total = f_results.getLong("F_SEATS_TOTAL");
			if (seats_total == 0) {
				System.out.println("total seats is zero!");
				return -1;
			}
			float seat_price = base_price + (base_price * (1 - (seats_left / seats_total)));
			PreparedStatement s_stmt = connect
					.prepareStatement("SELECT R_ID, R_F_ID, R_SEAT FROM RESERVATION WHERE R_F_ID = ?");
			s_stmt.setLong(1, f_id);
			ResultSet s_results = s_stmt.executeQuery();

			while (s_results.next()) {
				int r_id = s_results.getInt(1);
				int seatnum = s_results.getInt(3);
				if (seatmap[seatnum] != -1) {
					System.out.println("ERROR_32: Duplicate seat reservation: R_ID=" + r_id + " seatnum: " + seatnum);
					return (Constants._NO_ERROR_MODE) ? 0 : 32;
				}
				seatmap[seatnum] = 1;
			}
			int ctr = 0;
			Object[][] returnResults = new Object[150][];
			for (int i = 0; i < seatmap.length; ++i) {
				if (seatmap[i] == -1) { // Charge more for the first seats
					double price = seat_price * (i < 10 ? 2.0 : 1.0);
					Object[] row = new Object[] { f_id, i, price };
					returnResults[ctr++] = row;
					if (ctr == returnResults.length)
						break;
				}
			}
			// for (Object[] o1 : returnResults) {
			// for (Object o2 : o1)
			// System.out.println(o2);
			// System.out.println("====================");
			// }
			// ❄❄❄❄❄❄❄❄❄❄❄❄❄❄❄
			// TXN SUCCESSFUL!
			// ❄❄❄❄❄❄❄❄❄❄❄❄❄❄❄
			return 0;
		} catch (

		Exception e) {
			throw e;
		} finally {

		}

	}
}
