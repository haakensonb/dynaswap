package org.openmrs.module.dynaswap.atallah;

import java.math.BigInteger;

import org.openmrs.util.Security;

public class CryptUtil {
	
	public static String hashFunc(String val1, String val2) {
		String message = new String();
		message = val1 + val2;
		// OpenMRS security class uses SHA-512 by default
		return Security.encodeString(message);
	}
	
	public static String hashFunc(String val1, String val2, String valOpt) {
		String message = new String();
		message = val1 + valOpt + val2;
		// OpenMRS security class uses SHA-512 by default
		return Security.encodeString(message);
	}
	
	public static String encrypt(String r_ij, String t_j, String k_j) {
		byte[] init = new byte[42];
		String message = t_j + k_j;
		byte[] key = decodeHexUsingBigInt(r_ij);
		String ciphertext = Security.encrypt(message, init, key);
		return ciphertext;
	}
	
	public static byte[] decodeHexUsingBigInt(String hexString) {
		byte[] byteArray = new BigInteger(hexString, 16).toByteArray();
		if (byteArray[0] == 0) {
			byte[] output = new byte[byteArray.length - 1];
			System.arraycopy(byteArray, 1, output, 0, output.length);
			return output;
		}
		return byteArray;
	}
	
}
