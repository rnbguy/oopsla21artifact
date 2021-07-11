package kv;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.sql.Connection;

public class Check {
	public static int check(Connection conn, int id) throws Exception {
		try {
			PreparedStatement stmt1 = conn.prepareStatement("SELECT balance FROM saving WHERE id = ? ");
			PreparedStatement stmt2 = conn.prepareStatement("SELECT balance FROM checking WHERE id = ? ");
			PreparedStatement stmt3 = conn.prepareStatement("SELECT balance FROM total WHERE id = ? ");
			stmt1.setInt(1, id);
			stmt2.setInt(1, id);
			stmt3.setInt(1, id);
			ResultSet results1 = stmt1.executeQuery();
			ResultSet results2 = stmt2.executeQuery();
			ResultSet results3 = stmt3.executeQuery();
			int saving_bal = results1.getInt("balance");
			int checking_bal = results2.getInt("balance");
			int total_bal = results3.getInt("balance");
			if (total_bal != (saving_bal + checking_bal))
				return 1;
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
