
/**
* The concurrentUsersProject was developed to calculate the maximum concurrent users per day from a usersTable
* The usersTable has the following format <Username, ActionID, Date> 
* (ActionId=1 for a login , ActionId=2 for logout)
* 
* The algorithm is taking under consideration the following:
* A user might logout of the system inappropriately, as a result an entry ActionId2 will not be imported to the database.
* Thus, there might be a sequence of "1" without "2" in between. See sample excel document.
* We have to skip those sequential values and count the user only once.
* 
*
* @author  Christos Liontos
* @version 1.0
* @since   18/08/2015
*/

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MainClass {

	public static void main(String[] args) {

		try {
			String driverName = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
			String serverName = "localhost";
			String portNumber = "1433";
			String mydatabase = serverName + ":" + portNumber + ";databaseName=Test";
			String url = "jdbc:sqlserver://" + mydatabase + ";integratedSecurity=true";
			Class.forName(driverName);
			// Create a connection to the database
			Connection con = DriverManager.getConnection(url);
			Statement m_Statement = con.createStatement();
			// Create a query to retrieve the results from the UsersTable
			String query = "SELECT * FROM Users";
			ResultSet m_ResultSet = m_Statement.executeQuery(query);

			// A List with the rows returned by the resultSet. The values of the columns will be separated by commas ","
			List<String> usersTableList = new ArrayList<String>();
			// A List with the distinct dates of the UsersTable. It will be
			// sorted by date
			ArrayList<String> distinctDates = new ArrayList<String>();
			// Concurrent users for each distinct date
			ArrayList<Integer> concurrentUsersPerDay = new ArrayList<Integer>();
			// The dates corresponding to the concurrentUsersPerDay ArrayList
			ArrayList<String> datesEachNumberOfConcurrentUsersWasAchieved = new ArrayList<String>();
			// Users that were logged in but not logged out and need to be promoted to the next day since they are still logged in
			// We use the tempDate to extract the distinct dates from the usersTable
			String tempDate = "";
			int counter = 0;
			// Used as a Boolean to check if there is actionId 1 before an
			// actionId 2
			int thereIsOneBeforeTwo = 0;
			// Boolean to check if we need to increment or de-increment the
			// counter
			boolean addCounter = true;
			// Integer to save the line we stopped. We do not want to start
			// searching from the beginning
			int startingRow = 0;

			// Iterate through the resultSet and save the rows in the usersTableList, columns separated by commas
			while (m_ResultSet.next()) {
				ResultSetClass rs = new ResultSetClass();
				if (m_ResultSet.getString(1) != null && m_ResultSet.getString(2) != null
						&& m_ResultSet.getString(3) != null) {
					rs.setUsername(m_ResultSet.getString(1));
					rs.setActionId(m_ResultSet.getString(2));
					rs.setDate(m_ResultSet.getString(3));
					usersTableList.add(
							m_ResultSet.getString(1) + "," + m_ResultSet.getString(2) + "," + m_ResultSet.getString(3));
				}
			}

			// Extract the distinct dates into the distinctDates ArrayList
			for (int k = 0; k < usersTableList.size(); k++) {
				String[] line = usersTableList.get(k).split(",");
				String date = line[2];
				// System.out.println(usersList.get(k));
				if (!tempDate.equals(date)) {
					distinctDates.add(date);
				}
				tempDate = date;
			}

			// For each distinct date we start calculating the concurrent users
			for (int i = 0; i < distinctDates.size(); i++) {
				String currentDate = distinctDates.get(i);
				String lastMaximumDate = "";
				int maxPerDay = 0;

				// Only for the first row, we have to find out if there are any users that were already logged in from a potential previous
				// day that is not included in the database
				if (i == 0) {
					for (int k = 0; k < usersTableList.size(); k++) {
						String[] line = usersTableList.get(k).split(",");
						String username = line[0];
						String actionId = line[1];
						String lineDate = line[2];

						// If actionId is 2 then we increment the counter
						if (actionId.equals("2.0") && lineDate.equals(currentDate)) {
							if (k == 0) {
								counter = counter + 1;
								if (counter > maxPerDay) {
									maxPerDay = counter;
								}
								// If actionId is 1 then we have to find if the previous instance of the same username was 1 or 2
								// If it was 1, we do not increment, if it was 2 we increment
							} else {
								for (int r = k - 1; r >= 0; r--) {
									String[] previousLine = usersTableList.get(r).split(",");
									String previousUsername = previousLine[0];
									String previousActionId = previousLine[1];
									String previousLineDate = previousLine[2];

									if (previousUsername.equals(username) && previousActionId.equals("1.0")
											&& previousLineDate.equals(currentDate)) {
										thereIsOneBeforeTwo = thereIsOneBeforeTwo + 1;
										break;
									}
								}
								if (thereIsOneBeforeTwo == 0) {
									counter = counter + 1;
									if (counter > maxPerDay) {
										maxPerDay = counter;
									}
								}
								thereIsOneBeforeTwo = 0;
							}
						}
					}
					System.out.println("Number of users that were logged in before the first day:  "
							+ distinctDates.get(i) + "  are  " + counter);
				}

				// Since we have the outbound of the previous days (number of users that were logged in before the first day of the
				// database we can add them to the first day and continue the process
				for (int l = startingRow; l < usersTableList.size(); l++) {

					String[] line = usersTableList.get(l).split(",");
					String username = line[0];
					String actionId = line[1];
					String lineDate = line[2];
					System.out.println("WORKING FOR DATE " + lineDate + " IN LINE " + l + " Starting row: " + startingRow);
					if (counter > maxPerDay) {
						maxPerDay = counter;
						lastMaximumDate = lineDate;
					}

					// System.out.println("Line Date " + lineDate + " Current Distinct Date " + currentDate);
					addCounter = true;

					if (lineDate.equals(currentDate)) {

						if (actionId.equals("1.0")) {

							if (l == 0) {
								addCounter = true;
							} else {
								for (int p = l - 1; p >= 0; p--) {
									String[] previousLine = usersTableList.get(p).split(",");
									String previousUsername = previousLine[0];
									String previousActionId = previousLine[1];
									// System.out.println("Searching for line l=" + l + ", Username " + username + "  ---- Current previous line p=" + p + "  Previous Usename " + previousUsername);

									if (previousUsername.equals(username) && previousActionId.equals("1.0")) {
										// System.out.println("AddCounter set to false");
										addCounter = false;
										break;
									} else if (previousUsername.equals(username) && previousActionId.equals("2.0")) {
										addCounter = true;
										// System.out.println("AddCounter set to true");
										break;
									}
								}
							}
							if (addCounter == true) {
								counter = counter + 1;
								System.out.println("Counter INCREASED to " + counter + " for line: " + l);
								if (counter > maxPerDay) {
									maxPerDay = counter;
									lastMaximumDate = lineDate;
								}
							}
						} else if (actionId.equals("2.0")) {
							counter = counter - 1;
							System.out.println("Counter DECREASED to " + counter + " for line: " + l);
							if (counter < 0) {
								counter = 0;
							}
							if ((counter + 1) > maxPerDay) {
								maxPerDay = counter + 1;
								lastMaximumDate = lineDate;
							}

						}
					} else {
						System.out.println("BREAK because line date = " + lineDate);
						startingRow = l;
						break;
					}
				}

				if (maxPerDay != 0 && !lastMaximumDate.equals("")) {
					System.out.print("DATE " + lastMaximumDate + " MAXIMUM USERS " + maxPerDay);
					concurrentUsersPerDay.add(maxPerDay);
					datesEachNumberOfConcurrentUsersWasAchieved.add(lastMaximumDate);
				}
				System.out.println("\nCounter for date " + distinctDates.get(i) + ": " + counter
						+ ". To be PROMOTED to the next day");
			}

			for (int h = 0; h < concurrentUsersPerDay.size(); h++) {
				System.out.println("Date: " + datesEachNumberOfConcurrentUsersWasAchieved.get(h)
						+ " - ConcurrentUsers: " + concurrentUsersPerDay.get(h));

			}

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			// Could not find the database driver
		} catch (SQLException e) {
			e.printStackTrace();
			// Could not connect to the database
		}

	}

}