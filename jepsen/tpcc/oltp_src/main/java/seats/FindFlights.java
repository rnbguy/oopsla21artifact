package seats;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class FindFlights {
	public static int findFlights(Connection connect, long depart_aid, long arrive_aid, Timestamp start_date,
			Timestamp end_date, float distance) throws Exception {
		try {

			final List<Long> arrive_aids = new ArrayList<Long>();
			arrive_aids.add(arrive_aid);
			final List<Object[]> finalResults = new ArrayList<Object[]>();
			if (distance > 0) {
				// First get the nearby airports for the departure and arrival cities
				PreparedStatement nearby_stmt = connect.prepareStatement(
						"SELECT * " + "  FROM AIRPORT_DISTANCE WHERE d_ap_id0 = ? AND d_distance <= ? ALLOW FILTERING");
				nearby_stmt.setLong(1, depart_aid);
				nearby_stmt.setFloat(2, distance);
				ResultSet nearby_results = nearby_stmt.executeQuery();

				while (nearby_results.next()) {
					long aid = nearby_results.getLong(1);
					nearby_results.getInt(2);
					arrive_aids.add(aid);
				} // WHILE

				nearby_results.close();
				int num_nearby = arrive_aids.size();
				if (num_nearby > 0) {
					PreparedStatement f_stmt1 = connect.prepareStatement(
							"SELECT F_ID, F_AL_ID, F_SEATS_LEFT, F_DEPART_AP_ID, F_DEPART_TIME, F_ARRIVE_AP_ID, F_ARRIVE_TIME "
									+ " FROM FLIGHT " + " WHERE F_DEPART_AP_ID = ? " + " AND F_DEPART_TIME > ? "
									+ " AND F_DEPART_TIME < ? " + " ALLOW FILTERING");
					// Set Parameters
					f_stmt1.setLong(1, depart_aid);
					f_stmt1.setTimestamp(2, start_date);
					f_stmt1.setTimestamp(3, end_date);

					ResultSet flightResults1 = f_stmt1.executeQuery();
					flightResults1.next();
					int i = 0;
					while (flightResults1.next() && i < 10) {
						int f_depart_airport = flightResults1.getInt("F_DEPART_AP_ID");
						int f_arrive_airport = flightResults1.getInt("F_ARRIVE_AP_ID");
						int f_al_id = flightResults1.getInt("F_AL_ID");
						PreparedStatement f_stmt2 = connect
								.prepareStatement("SELECT AL_NAME, AL_IATTR00, AL_IATTR01 FROM AIRLINE WHERE AL_ID=?");
						f_stmt2.setInt(1, f_al_id);
						ResultSet flightResults2 = f_stmt2.executeQuery();
						boolean adv = flightResults2.next();
						if (!adv) {
							System.out
									.println(String.format("ERROR_21: airline with al_id=%d does not exist", f_al_id));
							return (Constants._NO_ERROR_MODE) ? 0 : 21;
						}
						String al_name = flightResults2.getString("AL_NAME");
						Object row[] = new Object[13];
						int r = 0;

						row[r++] = flightResults1.getInt("F_ID"); // [00] F_ID
						row[r++] = flightResults1.getInt("F_SEATS_LEFT"); // [01] SEATS_LEFT
						row[r++] = al_name;

						// DEPARTURE AIRPORT
						PreparedStatement ai_stmt1 = connect.prepareStatement(
								"SELECT AP_CODE, AP_NAME, AP_CITY, AP_LONGITUDE, AP_LATITUDE, AP_CO_ID "
										+ " FROM AIRPORT WHERE AP_ID = ? ");
						ai_stmt1.setInt(1, f_depart_airport);
						ResultSet ai_results1 = ai_stmt1.executeQuery();
						adv = ai_results1.next();
						if (!adv) {
							System.out.println(String.format("ERROR_22: departure airport with AP_ID=%d does not exist",
									f_depart_airport));
							return (Constants._NO_ERROR_MODE) ? 0 : 22;
						}
						long countryId = ai_results1.getLong("AP_CO_ID");
						PreparedStatement ai_stmt2 = connect.prepareStatement(
								"SELECT CO_ID, CO_NAME, CO_CODE_2, CO_CODE_3 " + " FROM COUNTRY WHERE CO_ID = ?");
						ai_stmt2.setLong(1, countryId);
						ResultSet ai_results2 = ai_stmt2.executeQuery(); // save the results boolean adv =
						ai_results2.next();
						row[r++] = flightResults1.getTimestamp("F_DEPART_TIME"); // [03] DEPART_TIME
						row[r++] = ai_results1.getString("AP_CODE"); // [04] DEPART_AP_CODE
						row[r++] = ai_results1.getString("AP_NAME"); // [05] DEPART_AP_NAME
						row[r++] = ai_results1.getString("AP_CITY"); // [06] DEPART_AP_CITY
						row[r++] = ai_results2.getString("CO_NAME"); // [07] DEPART_AP_COUNTRY

						// ARRIVAL AIRPORT
						PreparedStatement ai_stmt3 = connect.prepareStatement(
								"SELECT AP_CODE, AP_NAME, AP_CITY, AP_LONGITUDE, AP_LATITUDE, AP_CO_ID "
										+ " FROM AIRPORT WHERE AP_ID = ? ");
						ai_stmt3.setInt(1, f_arrive_airport);
						ResultSet ai_results3 = ai_stmt3.executeQuery();
						adv = ai_results3.next();
						if (!adv) {
							System.out.println(String.format("ERROR_23: arrival airport with AP_ID=%d does not exist",
									f_arrive_airport));
							return (Constants._NO_ERROR_MODE) ? 0 : 23;
						}
						long countryId2 = ai_results3.getLong("AP_CO_ID");
						PreparedStatement ai_stmt4 = connect.prepareStatement(
								"SELECT CO_ID, CO_NAME, CO_CODE_2, CO_CODE_3 " + " FROM COUNTRY WHERE CO_ID = ?");
						ai_stmt4.setLong(1, countryId2);
						ResultSet ai_results4 = ai_stmt4.executeQuery();
						ai_results4.next();
						row[r++] = flightResults1.getTimestamp("F_ARRIVE_TIME"); // [08] ARRIVE_TIME row[r++] =
						ai_results3.getString("AP_CODE"); // [09] ARRIVE_AP_CODE row[r++] =
						ai_results3.getString("AP_NAME"); // [10] ARRIVE_AP_NAME row[r++] =
						ai_results3.getString("AP_CITY"); // [11] ARRIVE_AP_CITY row[r++] =
						ai_results4.getString("CO_NAME"); // [12] ARRIVE_AP_COUNTRY
						finalResults.add(row);
						i++;
					}
				}
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
