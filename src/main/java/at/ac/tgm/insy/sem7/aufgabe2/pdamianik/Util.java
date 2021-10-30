package at.ac.tgm.insy.sem7.aufgabe2.pdamianik;

import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.*;

import static java.sql.Types.*;

public class Util {
	/**
	 * Converts a database result set into a {@link JSONArray}
	 * @param rs the {@link ResultSet} to convert
	 * @return The rs {@link ResultSet} to convert
	 * @throws SQLException if some data couldn't be correctly parsed from the {@link ResultSet}
	 */

	public static JSONArray dbTableToJSON(ResultSet rs) throws SQLException {
		ResultSetMetaData metaData = rs.getMetaData();

		JSONArray res = new JSONArray();

		while (rs.next()) {
			JSONObject obj = new JSONObject();
			for (int i = 1; i <= metaData.getColumnCount(); i++) {
				String name = metaData.getColumnName(i);
				switch (metaData.getColumnType(i)) {
					case CHAR:
					case VARCHAR:
					case LONGNVARCHAR:
						obj.put(name, rs.getString(i));
						break;
					case INTEGER:
					case BIGINT:
						obj.put(name, rs.getInt(i));
						break;
					case DECIMAL:
						obj.put(name, rs.getFloat(i));
						break;
					case TIMESTAMP:
					case TIMESTAMP_WITH_TIMEZONE:
						obj.put(name, rs.getTimestamp(i));
						break;
					default:
						System.err.println("Unhandled datatype: " + metaData.getColumnTypeName(i));
				}
			}
			res.put(obj);
		}
		return res;
	}

	/**
	 * Executes an SQL Query and returns the {@link ResultSet} converted into a {@link JSONArray}
	 * @param conn the connection that gets queried
	 * @param query the query to execute
	 * @return the query result converted into a {@link JSONArray}
	 */

	public static JSONArray queryToJSON(Connection conn, String query) {
		JSONArray res;

		try (Statement stmt = conn.createStatement()) {
			ResultSet rs = stmt.executeQuery(query);
			res = Util.dbTableToJSON(rs);
		} catch (SQLException e) {
			e.printStackTrace();
			res = new JSONArray();
		}

		return res;
	}
}
