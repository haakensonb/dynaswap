package org.openmrs.module.dynaswap.atallah;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.ArrayList;

/**
 * SelfAuthentication
 * 
 * For self-authenticating encryption/decryption used in the Atallah DAG hierarchy.
 * This method assumes that the role to object field mapping is private.
 */
public class SelfAuthentication {

	// For now, this will take arguments for data as a 2d array and columns(fields) as an array.
	// In the future, this data will come from the database and the structure may change.
	public static ArrayList<ArrayList<String>> encrypt(HashMap<String, CryptNode> nodeMapping, HashMap<String, ArrayList<String>> roleFieldMapping, ArrayList<ArrayList<String>> data, ArrayList<String> columns){
		// for every node, get its private key
		for (HashMap.Entry<String, CryptNode> entry : nodeMapping.entrySet()){
			String privateKey = entry.getValue().getDecryptKey();
			String nodeName = entry.getKey();
			// for every obj this node is mapped to
			ArrayList<String> Objects = roleFieldMapping.get(nodeName);
			for (String obj : Objects){
				// for every row in the data
				// chose the correct column(field) and selfAuthEncHelper
				int columnNum = columns.indexOf(obj);
				for (int i = 0; i < data.size(); i++){
					String curColData = data.get(i).get(columnNum);
					String encryptedColData = SelfAuthentication.selfAuthEncHelper(privateKey, curColData);
					data.get(i).set(columnNum, encryptedColData);
				}
			}
		}
		return data;
	}

	// Decrypt using DAG without knowing role-to-object field mapping
	public static ArrayList<ArrayList<String>> decrypt(HashMap<String, CryptNode> nodeMapping, ArrayList<ArrayList<String>> data, ArrayList<String> columns, String sourceNode, String targetCol){
		// Don't have a public role-to-object mapping, so we have to derive all descendant keys.
		String deriveKey = nodeMapping.get(sourceNode).getDeriveKey();
		ArrayList<String> privateKeys = 

	}

	// The methods for deriving keys should probably be placed on the CryptDAG class.
	// But for now, I will just make them take a nodeMapping HashMap as input.
	public static ArrayList<String> deriveDescKey(HashMap<String, CryptNode> nodeMapping, String srcNode, String deriveKey){
		ArrayList<String> descKeys = new ArrayList<String>();
		descKeys.add(nodeMapping.get(srcNode).getDecryptKey());
		for (String key : SelfAuthentication.deriveDescKeyHelper(nodeMapping, srcNode, deriveKey)){
			descKeys.add(key);
		}
		// Remove any duplicates from the list before returning
		ArrayList<String> newDescKeys = descKeys.stream()
												.distinct()
												.collect(Collectors.toList());
		return newDescKeys;
	}

	public static ArrayList<String> deriveDescKeyHelper(HashMap<String, CryptNode> nodeMapping, String srcNode, String deriveKey){
		ArrayList<String> descKeys = new ArrayList<String>();
		for (CryptEdge edge : nodeMapping.get(srcNode).edges){
			CryptNode childNode = nodeMapping.get(edge.childName);
			String ciphertext = CryptUtil.hashFunc(deriveKey, childNode.label);
			// Will return t_j concated with k_j
			// In other words, it's the derive key and decrypt key which must be split apart.
			String plaintext = CryptUtil.decrypt(ciphertext, edge.y_ij);
			String curDeriveKey = plaintext.substring(0, deriveKey.length());
			String curDecryptKey = plaintext.substring(deriveKey.length());
			descKeys.add(curDecryptKey);
			for (String key : SelfAuthentication.deriveDescKey(nodeMapping, childNode.name, curDeriveKey)){
				descKeys.add(key);
			}
		}
		return descKeys;
	}

	public static String selfAuthEncHelper(String privateKey, String data){
		String formattedMessage = privateKey.concat(data);
		String encryptedMessage = CryptUtil.encrypt(privateKey, formattedMessage);
		return encryptedMessage;
	}
	
}