package org.openmrs.module.dynaswap.atallah;

import java.sql.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

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
			
			rs = stmt.executeQuery("select person_id, birthdate, gender from person");
			while (rs.next()) {
				ArrayList<String> row = new ArrayList<String>();
				String id = rs.getString("person_id");
				row.add(id);
				String birthdate = rs.getString("birthdate");
				row.add(birthdate);
				String gender = rs.getString("gender");
				row.add(gender);
				System.out.println("Row: " + id + ", " + birthdate + ", " + gender);
				resultList.add(row);
			}
			conn.close();
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
		
		return resultList;
	}
	
	public ArrayList<String> getColumnNames() {
		ArrayList<String> columnNames = new ArrayList<String>();
		
		try {
			Connection conn = DriverManager.getConnection(this.url, this.username, this.password);
			ResultSet columns = conn.getMetaData().getColumns(null, null, "person", null);
			while (columns.next()) {
				columnNames.add(columns.getString("COLUMN_NAME"));
			}
			conn.close();
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
		
		return columnNames;
	}
}
