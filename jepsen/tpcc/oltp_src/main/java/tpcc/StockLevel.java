package tpcc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import java.sql.Connection;

public class StockLevel {
	private static boolean _VERBOSE = false;

	public static int stockLevel(Connection conn, int w_id, int d_id, double threshold) throws Exception {
		PreparedStatement stmt = null;
		try {

			//
			// retrieve the latest order_id from the given district
			stmt = conn.prepareStatement(
					"SELECT D_NEXT_O_ID " + "  FROM " + "DISTRICT" + " WHERE D_W_ID = ? " + "   AND D_ID = ?");
			stmt.setInt(1, w_id);
			stmt.setInt(2, d_id);
			ResultSet d_rs = stmt.executeQuery();
			if (!d_rs.next()) {
				System.out.println("ERROR_51: district does not exist: " + w_id + "," + d_id);
				return 51;
			}
			int o_id = d_rs.getInt("D_NEXT_O_ID");
			d_rs.close();

			//
			// retrieve the latest 20 orders
			stmt = conn.prepareStatement(
					"SELECT ol_i_id FROM " + "ORDER_LINE" + " WHERE ol_w_id=? and ol_d_id=? and OL_O_ID < ? and ol_o_id > ?");
			stmt.setInt(1, w_id);
			stmt.setInt(2, d_id);
			stmt.setInt(3, o_id);
			stmt.setInt(4, (o_id - 20));
			ResultSet ol_rs = stmt.executeQuery();
			List<Integer> all_ol_i_ids = new ArrayList<Integer>();
			while (ol_rs.next()) {
				all_ol_i_ids.add(ol_rs.getInt("ol_i_id"));
			}
			@SuppressWarnings("unchecked")
			List<Integer> ditinct_ol_i_ids = (List<Integer>) (List<?>) all_ol_i_ids.stream().distinct()
					.collect(Collectors.toList());
			String in_clause = Utils_tpcc.get_in_clause(ditinct_ol_i_ids);
			stmt = conn.prepareStatement("SELECT * FROM STOCK WHERE " + "s_w_id=? " + "AND S_QUANTITY < ? "
					+ "AND s_i_id IN " + in_clause + "");
			stmt.setInt(1, w_id);
			stmt.setDouble(2, threshold);
			ResultSet s_rs = stmt.executeQuery();
			if (_VERBOSE)
				while (s_rs.next()) {
					System.out.println("low stock found: " + s_rs.getInt("s_i_id"));
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
