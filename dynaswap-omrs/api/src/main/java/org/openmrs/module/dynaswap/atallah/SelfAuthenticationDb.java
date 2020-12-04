package org.openmrs.module.dynaswap.atallah;

import java.sql.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

import org.openmrs.module.dynaswap.atallah.SelfAuthentication;

public class SelfAuthenticationDb {
	
	// Hacky and hardcoded with test values for now.
	private String url;
	
	private String username;
	
	private String password;
	
	public SelfAuthenticationDb() {
		this.url = "jdbc:mysql://0.0.0.0:3308/server1?useSSL=false&allowPublicKeyRetrieval=true";
		this.username = "root";
		this.password = "Admin123";
	}
	
	// public List<Map<String, Object>> getList() {
	// 	return this.jdbcTemplate.queryForList("select person_id, birthdate, gender from person");
	// }
	
	public ArrayList<ArrayList<String>> getList() {
		ArrayList<ArrayList<String>> resultList = new ArrayList<ArrayList<String>>();
		
		try {
			Connection conn = DriverManager.getConnection(this.url, this.username, this.password);
			Statement stmt = conn.createStatement();
			ResultSet rs;
			
			// rs = stmt.executeQuery("select birthdate, gender from person");
			// while (rs.next()) {
			// 	ArrayList<String> row = new ArrayList<String>();
			// 	// String id = rs.getString("person_id");
			// 	// row.add(id);
			// 	String birthdate = rs.getString("birthdate");
			// 	row.add(birthdate);
			// 	String gender = rs.getString("gender");
			// 	row.add(gender);
			// 	System.out.println("Row: " + birthdate + ", " + gender);
			// 	resultList.add(row);
			// }
			
			rs = stmt.executeQuery("select obs_id, value_text from obs");
			while (rs.next()) {
				String id = rs.getString("obs_id");
				String value_text = rs.getString("value_text");
				System.out.println("Row: " + id + ", " + value_text);
				if (value_text != null) {
					ArrayList<String> row = new ArrayList<String>();
					row.add(id);
					row.add(value_text);
					resultList.add(row);
				}
			}
			
			conn.close();
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
		
		return resultList;
	}
	
	public void updateTable(ArrayList<ArrayList<String>> data, ArrayList<String> primaryKeys) {
		try {
			Connection conn = DriverManager.getConnection(this.url, this.username, this.password);
			for (int i = 0; i < data.size(); i++) {
				PreparedStatement ps = conn
				// .prepareStatement("UPDATE person SET birthdate = ?, gender = ? WHERE person_id = ?");
				        .prepareStatement("UPDATE obs SET value_text = ? WHERE obs_id = ?");
				int index = 0;
				for (int j = 0; j < data.get(i).size(); j++) {
					String val = data.get(i).get(j);
					// PreparedStatement index starts at 1.
					index = j + 1;
					ps.setString(index, val);
				}
				// Assuming that each rows of data and primary keys stay in the same ordering.
				ps.setString(index + 1, primaryKeys.get(i));
				ps.executeUpdate();
				ps.close();
			}
			
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	
	public ArrayList<ArrayList<String>> decryptDatabaseData(HashMap<String, CryptNode> nodeMapping,
	        ArrayList<ArrayList<String>> data, ArrayList<String> primaryKeys, ArrayList<String> columns, String sourceNode,
	        String targetCol) {
		ArrayList<ArrayList<String>> decryptedData = new ArrayList<ArrayList<String>>();
		try {
			decryptedData = SelfAuthentication.decrypt(nodeMapping, data, columns, sourceNode, targetCol);
			// System.out.println("PRINTING DECRYPTED DATA:");
			// String arrStr = "";
			// for (int i = 0; i < decryptedData.size(); i++) {
			// 	for (int j = 0; j < decryptedData.get(i).size(); j++) {
			// 		arrStr += decryptedData.get(i).get(j);
			// 		arrStr += " ";
			// 	}
			// 	arrStr += "\n";
			// }
			// System.out.println(arrStr);
			
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
		
		return decryptedData;
		
	}
	
	public ArrayList<String> getPrimaryKeys() {
		ArrayList<String> primaryKeys = new ArrayList<String>();
		
		try {
			Connection conn = DriverManager.getConnection(this.url, this.username, this.password);
			Statement stmt = conn.createStatement();
			ResultSet rs;
			
			// rs = stmt.executeQuery("SELECT person_id FROM person");
			rs = stmt.executeQuery("SELECT obs_id FROM obs");
			while (rs.next()) {
				String key = rs.getString("obs_id");
				primaryKeys.add(key);
			}
			conn.close();
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
		
		return primaryKeys;
	}
	
	public ArrayList<String> getColumnNames() {
		ArrayList<String> columnNames = new ArrayList<String>();
		
		try {
			Connection conn = DriverManager.getConnection(this.url, this.username, this.password);
			ResultSet columns = conn.getMetaData().getColumns(null, null, "obs", null);
			while (columns.next()) {
				columnNames.add(columns.getString("COLUMN_NAME"));
			}
			conn.close();
			// Assume that first column will always be primary key?
			// And this can't be used with the encryption
			columnNames.remove(0);
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
		
		return columnNames;
	}
	
	public ArrayList<String> separatePrimaryKeysFromData(ArrayList<ArrayList<String>> results) {
		ArrayList<String> primaryKeys = new ArrayList<String>();
		
		for (int i = 0; i < results.size(); i++) {
			String key = results.get(i).remove(0);
			primaryKeys.add(key);
		}
		
		return primaryKeys;
	}
}
