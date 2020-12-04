package org.openmrs.module.dynaswap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.junit.Test;
import org.openmrs.module.dynaswap.atallah.CryptDAG;
import org.openmrs.module.dynaswap.atallah.SelfAuthentication;
import org.openmrs.module.dynaswap.atallah.SelfAuthenticationDb;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.module.dynaswap.CryptDAGTest;
import org.openmrs.module.dynaswap.atallah.CryptNode;
import org.openmrs.module.dynaswap.TestSetupUtil;

public class SelfAuthenticationDbTest extends BaseModuleContextSensitiveTest {
	
	@Test
	public void SelfAuthenticationDbTest_getList() {
		System.out.println("SelfAuth using Db values");
		CryptDAGTest.setupSimpleModel();
		CryptDAG dag = new CryptDAG();
		dag.createGraph();
		HashMap<String, CryptNode> nodeMapping = dag.getNodeMapping();
		SelfAuthenticationDb selfAuthDb = new SelfAuthenticationDb();
		long startTime = System.currentTimeMillis();
		ArrayList<ArrayList<String>> databaseData = selfAuthDb.getList();
		System.out.println("Original Database data:");
		TestSetupUtil.print2dArrayList(databaseData);
		ArrayList<String> primaryKeys = selfAuthDb.separatePrimaryKeysFromData(databaseData);
		// ArrayList<String> columns = selfAuthDb.getColumnNames();
		ArrayList<String> columns = new ArrayList<String>(Arrays.asList("value_text"));
		System.out.println("columns:");
		for (String col : columns) {
			System.out.println(col);
		}
		HashMap<String, HashMap<String, ArrayList<String>>> roleFieldMapping = dag.getRoleDataMap();
		ArrayList<ArrayList<String>> result = SelfAuthentication.encrypt(nodeMapping, roleFieldMapping, "obs", databaseData,
		    columns);
		System.out.println("Enc results:");
		TestSetupUtil.print2dArrayList(result);
		// ArrayList<String> primaryKeys = selfAuthDb.getPrimaryKeys();
		
		selfAuthDb.updateTable(result, primaryKeys);
		ArrayList<ArrayList<String>> updatedDatabaseData = selfAuthDb.getList();
		System.out.println("Updated Database data");
		TestSetupUtil.print2dArrayList(updatedDatabaseData);
		
		// Should use OpenMRS to get current role and then check nodemapping
		String sourceNode = "[CRYPT]admin";
		String targetCol = "value_text";
		ArrayList<String> updatedPrimaryKeys = selfAuthDb.separatePrimaryKeysFromData(updatedDatabaseData);
		ArrayList<ArrayList<String>> decryptedData = selfAuthDb.decryptDatabaseData(nodeMapping, updatedDatabaseData,
		    updatedPrimaryKeys, columns, sourceNode, targetCol);
		if (!decryptedData.isEmpty()) {
			selfAuthDb.updateTable(decryptedData, updatedPrimaryKeys);
			selfAuthDb.getList();
		}
		long endTime = System.currentTimeMillis();
		System.out.println("Time: " + (endTime - startTime));
		
	}
	
}
