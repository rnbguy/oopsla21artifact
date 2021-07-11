package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Iterator;

public class CassConn {
	private static RoundRobin<Connection> connectionPool = new RoundRobin<Connection>();
	private static Iterator<Connection> connections = connectionPool.iterator();
	private static int _NUMBER_OF_CONNECTIONS_PER_NODE = 4;

	public static void prepareConnections(int n, int c, String bench) {
		try {
			Class.forName("org.postgresql.Driver");
			for (int i = 1; i <= n; i++)
				for (int j = 0; j < _NUMBER_OF_CONNECTIONS_PER_NODE; j++) {
					Connection connect = (Connection) DriverManager.getConnection("jdbc:postgresql://"
							+ "n" + String.valueOf(i) + ":9042/" + bench, "postgres", "123");
					connectionPool.add(connect);
				}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static Connection getConnection(String localAddr) {
		Connection connect = null;
		connect = connections.next();
		// System.out.println("GET CONNECTION: " + connect.getClusterMetadata());
		return connect;
	}

	public static void closeConnection(Connection connection) {
		try {
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
