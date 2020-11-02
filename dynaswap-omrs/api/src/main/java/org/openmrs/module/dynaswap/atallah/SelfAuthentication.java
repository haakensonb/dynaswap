package org.openmrs.module.dynaswap.atallah;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Random;

/**
 * SelfAuthentication For self-authenticating encryption/decryption used in the Atallah DAG
 * hierarchy. This method assumes that the role to object field mapping is private.
 */
public class SelfAuthentication {
	
	// For now, this will take arguments for data as a 2d array and columns(fields) as an array.
	// In the future, this data will come from the database and the structure may change.
	public static ArrayList<ArrayList<String>> encrypt(HashMap<String, CryptNode> nodeMapping,
	        HashMap<String, ArrayList<String>> roleFieldMapping, ArrayList<ArrayList<String>> data, ArrayList<String> columns) {
		// for every node, get its private key
		for (HashMap.Entry<String, CryptNode> entry : nodeMapping.entrySet()) {
			String privateKey = entry.getValue().getDecryptKey();
			String nodeName = entry.getKey();
			// for every obj this node is mapped to
			ArrayList<String> Objects = roleFieldMapping.get(nodeName);
			for (String obj : Objects) {
				// for every row in the data
				// chose the correct column(field) and selfAuthEncHelper
				int columnNum = columns.indexOf(obj);
				for (int i = 0; i < data.size(); i++) {
					String curColData = data.get(i).get(columnNum);
					String encryptedColData = SelfAuthentication.selfAuthEncHelper(privateKey, curColData);
					data.get(i).set(columnNum, encryptedColData);
				}
			}
		}
		return data;
	}
	
	public static String getValidTargetCol(HashMap<String, ArrayList<String>> roleFieldMapping, String sourceNode,
	        String targetCol, HashMap<String, CryptNode> nodeMapping, ArrayList<String> columns) {
		// Ensure that the source node choosen can actually decrypt the target column.
		// Useful when testing SelfAuth using randomly choosen source nodes.
		boolean compatible = false;
		while (!compatible) {
			String targetNode = "";
			for (Map.Entry<String, ArrayList<String>> entry : roleFieldMapping.entrySet()) {
				String nodeName = entry.getKey();
				ArrayList<String> cols = entry.getValue();
				if (cols.contains(targetCol)) {
					targetNode = nodeName;
					break;
				}
			}
			ArrayList<String> path = SelfAuthentication.getPath(sourceNode, targetNode, nodeMapping);
			// if (path.size() > 0) {
			if (!path.isEmpty()) {
				compatible = true;
			} else {
				Random rand = new Random();
				targetCol = columns.get(rand.nextInt(columns.size()));
			}
		}
		return targetCol;
	}
	
	// Decrypt using DAG without knowing role-to-object field mapping
	public static ArrayList<ArrayList<String>> decrypt(HashMap<String, CryptNode> nodeMapping,
	        ArrayList<ArrayList<String>> data, ArrayList<String> columns, String sourceNode, String targetCol) {
		// Don't have a public role-to-object mapping, so we have to derive all descendant keys.
		String deriveKey = nodeMapping.get(sourceNode).getDeriveKey();
		ArrayList<String> privateKeys = SelfAuthentication.deriveDescKey(nodeMapping, sourceNode, deriveKey);
		
		// Get encrypted data we want and initialize decrypted data
		int columnIndex = columns.indexOf(targetCol);
		// ArrayList<String> encryptedData = data.get(columnIndex);
		ArrayList<String> encryptedData = SelfAuthentication.getColumnCopyFrom2d(data, columnIndex);
		System.out.println("ENC column: ");
		for (String d : encryptedData) {
			System.out.println(d);
		}
		ArrayList<String> decryptedData = new ArrayList();
		
		// Figure what key (if any) work
		String targetKey = "";
		for (String privateKey : privateKeys) {
			String ciphertext = encryptedData.get(0);
			String plaintext = CryptUtil.decrypt(privateKey, ciphertext);
			System.out.println("SelfAUTH plaintext: " + plaintext);
			System.out.println("SelfAUTH plaintext len: " + plaintext.length());
			// String plaintextKey = plaintext.substring(0, privateKey.length());
			String plaintextKey = "";
			if (!plaintext.isEmpty()) {
				plaintextKey = plaintext.substring(0, privateKey.length());
			}
			if (privateKey.equals(plaintextKey)) {
				targetKey = plaintextKey;
				break;
			}
		}
		
		// Use working key to decrypt target column data
		if (!targetKey.isEmpty()) {
			for (String element : encryptedData) {
				String plaintext = CryptUtil.decrypt(targetKey, element);
				String plaintextData = plaintext.substring(targetKey.length());
				decryptedData.add(plaintextData);
			}
		}
		
		// Replace original data with decrypted column and return.
		data.set(columnIndex, decryptedData);
		return data;
	}
	
	// The methods for deriving keys/paths should probably be placed on the CryptDAG class.
	// But for now, I will just make them take a nodeMapping HashMap as input.
	public static ArrayList<String> deriveDescKey(HashMap<String, CryptNode> nodeMapping, String srcNode, String deriveKey) {
		ArrayList<String> descKeys = new ArrayList<String>();
		descKeys.add(nodeMapping.get(srcNode).getDecryptKey());
		for (String key : SelfAuthentication.deriveDescKeyHelper(nodeMapping, srcNode, deriveKey)) {
			descKeys.add(key);
		}
		// Remove any duplicates from the list before returning
		ArrayList<String> newDescKeys = SelfAuthentication.removeDuplicates(descKeys);
		return newDescKeys;
	}
	
	public static ArrayList<String> deriveDescKeyHelper(HashMap<String, CryptNode> nodeMapping, String srcNode,
	        String deriveKey) {
		System.out.println("deriveKey: " + deriveKey);
		ArrayList<String> descKeys = new ArrayList<String>();
		for (CryptEdge edge : nodeMapping.get(srcNode).edges) {
			CryptNode childNode = nodeMapping.get(edge.childName);
			System.out.println("parentNode: " + nodeMapping.get(srcNode).getName());
			System.out.println("childNode: " + childNode.getName());
			System.out.println("childNodeLabel: " + childNode.label);
			String hashedKey = CryptUtil.hashFunc(deriveKey, childNode.label);
			System.out.println("hashedKey: " + hashedKey);
			// Will return t_j concated with k_j
			// In other words, it's the derive key and decrypt key which must be split apart.
			System.out.println("y_ij: " + edge.y_ij);
			String deriveAndDecryptKeys = CryptUtil.decrypt(hashedKey, edge.y_ij);
			System.out.println("deriveAndDecryptKeys: " + deriveAndDecryptKeys);
			String curDeriveKey = deriveAndDecryptKeys.substring(0, deriveKey.length());
			System.out.println("curDeriveKey: " + curDeriveKey);
			String curDecryptKey = deriveAndDecryptKeys.substring(deriveKey.length());
			System.out.println("curDecryptKey: " + curDecryptKey);
			descKeys.add(curDecryptKey);
			for (String key : SelfAuthentication.deriveDescKey(nodeMapping, childNode.name, curDeriveKey)) {
				descKeys.add(key);
			}
		}
		return descKeys;
	}
	
	public static ArrayList<String> getPath(String srcNode, String destNode, HashMap<String, CryptNode> nodeMapping) {
		ArrayList<String> currentPath = new ArrayList<String>(Arrays.asList(srcNode));
		if (SelfAuthentication.getPathHelper(srcNode, destNode, currentPath, nodeMapping)) {
			return currentPath;
		}
		return new ArrayList<String>();
	}
	
	public static boolean getPathHelper(String srcNode, String destNode, ArrayList<String> currentPath,
	        HashMap<String, CryptNode> nodeMapping) {
		if (srcNode.equals(destNode)) {
			return true;
		}
		for (CryptEdge edge : nodeMapping.get(srcNode).edges) {
			// Does this work?
			// String edgeParentName = edge.parentName;
			String edgeChildName = edge.childName;
			currentPath.add(edgeChildName);
			if (SelfAuthentication.getPathHelper(edgeChildName, destNode, currentPath, nodeMapping)) {
				return true;
			}
			currentPath.remove(currentPath.size() - 1);
		}
		return false;
	}
	
	public static ArrayList<String> removeDuplicates(ArrayList<String> list) {
		// Place all items in an ordered set to remove duplicates.
		Set<String> set = new LinkedHashSet<String>();
		set.addAll(list);
		// Then clear the original set and refill with non-duplicated values.
		list.clear();
		list.addAll(set);
		return list;
	}
	
	public static String selfAuthEncHelper(String privateKey, String data) {
		String formattedMessage = privateKey.concat(data);
		String encryptedMessage = CryptUtil.encrypt(privateKey, formattedMessage);
		return encryptedMessage;
	}
	
	public static ArrayList<String> getColumnCopyFrom2d(ArrayList<ArrayList<String>> data, int columnIndex) {
		ArrayList<String> column = new ArrayList<String>();
		for (int rowIndex = 0; rowIndex < data.size(); rowIndex++) {
			column.add(data.get(rowIndex).get(columnIndex));
		}
		return column;
	}
	
}
