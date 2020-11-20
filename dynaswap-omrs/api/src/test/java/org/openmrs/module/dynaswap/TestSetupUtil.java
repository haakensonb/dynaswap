package org.openmrs.module.dynaswap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;

public class TestSetupUtil {
	
	// For now some simple test data values will be copied over and hard coded.
	public static ArrayList<ArrayList<String>> getSimpleData() {
		int rows = 30;
		ArrayList<ArrayList<String>> data = new ArrayList<ArrayList<String>>(rows);
		for (int i = 0; i < rows; i++) {
			data.add(new ArrayList<String>(Arrays.asList("no-recurrence-events", "30-39", "premeno", "30-34", "0-2", "no",
			    "3", "left", "left_low", "no")));
		}
		return data;
	}
	
	// For now some simple test data values will be copied over and hard coded.
	public static ArrayList<String> getSimpleDataColumns() {
		ArrayList<String> columnNames = new ArrayList<String>(Arrays.asList("Object 1", "Object 2", "Object 3", "Object 4",
		    "Object 5", "Object 6", "Object 7", "Object 8", "Object 9", "Object 1"));
		return columnNames;
	}
	
	public static HashMap<String, HashMap<String, ArrayList<String>>> getSimpleRoleFieldMapping() {
		HashMap<String, HashMap<String, ArrayList<String>>> roleFieldMapping = new HashMap<String, HashMap<String, ArrayList<String>>>();
		// For now try simple 1-1 mapping.
		// What about placeholders?
		roleFieldMapping.put("[CRYPT]admin", new HashMap<String, ArrayList<String>>());
		roleFieldMapping.put("[CRYPT]doctor", new HashMap<String, ArrayList<String>>());
		roleFieldMapping.put("[CRYPT]clerk", new HashMap<String, ArrayList<String>>());
		roleFieldMapping.put("[CRYPT]nurse", new HashMap<String, ArrayList<String>>());
		roleFieldMapping.put("Placeholder0", new HashMap<String, ArrayList<String>>());
		roleFieldMapping.put("Placeholder1", new HashMap<String, ArrayList<String>>());
		
		roleFieldMapping.get("[CRYPT]admin").put("testTable", new ArrayList<String>(Arrays.asList("Object 1")));
		roleFieldMapping.get("[CRYPT]doctor").put("testTable", new ArrayList<String>(Arrays.asList("Object 2")));
		roleFieldMapping.get("[CRYPT]clerk").put("testTable", new ArrayList<String>(Arrays.asList("Object 3")));
		roleFieldMapping.get("[CRYPT]nurse").put("testTable", new ArrayList<String>(Arrays.asList("Object 4")));
		roleFieldMapping.get("Placeholder0").put("testTable", new ArrayList<String>(Arrays.asList("Object 5")));
		roleFieldMapping.get("Placeholder1").put("testTable", new ArrayList<String>(Arrays.asList("Object 6")));
		
		return roleFieldMapping;
	}
	
	public static void print2dArrayList(ArrayList<ArrayList<String>> arr) {
		String arrStr = "";
		for (int i = 0; i < arr.size(); i++) {
			for (int j = 0; j < arr.get(i).size(); j++) {
				arrStr += arr.get(i).get(j);
				arrStr += " ";
			}
			arrStr += "\n";
		}
		System.out.println(arrStr);
	}
}
