package org.openmrs.module.dynaswap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

import org.junit.Test;
import org.openmrs.module.dynaswap.atallah.CryptDAG;
import org.openmrs.module.dynaswap.atallah.CryptNode;
import org.openmrs.module.dynaswap.atallah.SelfAuthentication;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.module.dynaswap.CryptDAGTest;
import org.openmrs.module.dynaswap.atallah.CryptUtil;
import org.openmrs.util.Security;
import org.openmrs.module.dynaswap.TestSetupUtil;

public class SelfAuthenticationTest extends BaseModuleContextSensitiveTest {
	
	@Test
	public void SelfAuthentication_constructor() {
		System.out.println("Self Auth test");
		CryptDAGTest.setupSimpleModel();
		CryptDAG dag = new CryptDAG();
		dag.createGraph();
		System.out.println(dag.getFormattedGraph());
		HashMap<String, CryptNode> nodeMapping = dag.getNodeMapping();
		SelfAuthentication selfAuth = new SelfAuthentication();
		ArrayList<ArrayList<String>> data = TestSetupUtil.getSimpleData();
		System.out.println("Original Data: ");
		TestSetupUtil.print2dArrayList(data);
		ArrayList<String> columns = TestSetupUtil.getSimpleDataColumns();
		HashMap<String, HashMap<String, ArrayList<String>>> roleFieldMapping = TestSetupUtil.getSimpleRoleFieldMapping();
		String tableName = "testTable";
		ArrayList<ArrayList<String>> encryptedData = SelfAuthentication.encrypt(nodeMapping, roleFieldMapping, tableName,
		    data, columns);
		System.out.println("Encrypted data:");
		TestSetupUtil.print2dArrayList(encryptedData);
		// Choose random starting node.
		Random rand = new Random();
		int numOfNodes = nodeMapping.keySet().size();
		String[] possibleNodes = new String[numOfNodes];
		possibleNodes = nodeMapping.keySet().toArray(possibleNodes);
		String sourceNode = possibleNodes[rand.nextInt(possibleNodes.length - 1)];
		// What about when there are multiple valid target cols?
		String randTargetCol = columns.get(rand.nextInt(columns.size() - 1));
		String targetCol = SelfAuthentication.getValidTargetCol(roleFieldMapping, tableName, sourceNode, randTargetCol,
		    nodeMapping, columns);
		System.out.println("sourceNode: " + sourceNode);
		System.out.println("Target column: " + targetCol);
		System.out.println("sourceNode mapped to field: "
		        + roleFieldMapping.get(sourceNode).get(tableName).contains(targetCol));
		ArrayList<ArrayList<String>> decryptedData = SelfAuthentication.decrypt(nodeMapping, encryptedData, columns,
		    sourceNode, targetCol);
		System.out.println("Decrypted data:");
		TestSetupUtil.print2dArrayList(decryptedData);
	}
	
}
