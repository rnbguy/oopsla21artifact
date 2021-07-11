package kv;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.sql.Connection;

public class DepositSaving {
	public static int deposit_saving(Connection conn, int id, int bal) throws Exception {
		try {
			PreparedStatement stmt = conn.prepareStatement("SELECT balance FROM saving WHERE id = ? ");
			PreparedStatement stmt2 = conn.prepareStatement("SELECT balance FROM total WHERE id = ? ");
			stmt.setInt(1, id);
			stmt2.setInt(1, id);
			ResultSet results = stmt.executeQuery();
			ResultSet results2 = stmt2.executeQuery();
			int old_bal = results.getInt("balance");
			int old_bal2 = results2.getInt("balance");
			stmt = conn.prepareStatement("UPDATE saving SET balance = ? WHERE id = ? ");
			stmt.setInt(1, old_bal + bal);
			stmt.setInt(2, id);
			stmt.executeUpdate();

			stmt2 = conn.prepareStatement("UPDATE total SET balance = ? WHERE id = ? ");
			stmt2.setInt(1, old_bal2 + bal);
			stmt2.setInt(2, id);
			stmt2.executeUpdate();

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
