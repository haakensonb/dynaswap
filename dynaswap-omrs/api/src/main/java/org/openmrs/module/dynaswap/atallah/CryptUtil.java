package org.openmrs.module.dynaswap.atallah;

import org.apache.commons.codec.digest.DigestUtils;
import org.openmrs.util.Security;

import java.math.BigInteger;

public class CryptUtil {
	
	public static String hashFunc(String val1, String val2) {
		String message = new String();
		message = val1 + val2;
		// return Security.encodeString(message, "SHA-256");
		return DigestUtils.sha256Hex(message);
	}
	
	public static String hashFunc(String val1, String val2, String valOpt) {
		String message = new String();
		message = val1 + valOpt + val2;
		// return Security.encodeString(message, "SHA-256");
		return DigestUtils.sha256Hex(message);
	}
	
	public static String encrypt(String r_ij, String t_j, String k_j) {
		String message = t_j + k_j;
		String ciphertext = CryptUtil.encrypt(r_ij, message);
		return ciphertext;
	}
	
	// More general form of encryption, rather than specific atallah formatted encryption
	public static String encrypt(String keyStr, String message) {
		byte[] init = Security.getSavedInitVector();
		byte[] key = hexStringToByteArray(keyStr);
		String ciphertext = Security.encrypt(message, init, key);
		return CryptUtil.strToHexStr(ciphertext);
	}
	
	public static String decrypt(String ciphertext, String keyHexStr) {
		System.out.println("decrypt meth ciphertext: " + ciphertext);
		System.out.println("decrypt meth keyHexStr: " + keyHexStr);
		byte[] init = Security.getSavedInitVector();
		byte[] key = hexStringToByteArray(keyHexStr);
		System.out.println("key hexStr: " + CryptUtil.bytesToHex(key));
		String plaintext = Security.decrypt(ciphertext, init, key);
		return plaintext;
	}
	
	// String must be an even length to be valid hex string.
	public static byte[] hexStringToByteArray(String hex) {
		// int len = str.length();
		// byte[] data = new byte[len / 2];
		// for (int i = 0; i < len; i += 2) {
		// 	data[i / 2] = (byte) ((Character.digit(str.charAt(i), 16) << 4) + Character.digit(str.charAt(i + 1), 16));
		// }
		// return data;
		
		int byteNum = hex.length() / 2;
		byte[] key = new byte[byteNum];
		// Using i as the distance from the END of the string.
		for (int i = 0; i < hex.length() && (i / 2) < byteNum; i++) {
			// Pull out the hex value of the character.
			int nibble = Character.digit(hex.charAt(hex.length() - 1 - i), 16);
			if ((i & 1) != 0) {
				// When i is odd we shift left 4.
				nibble = nibble << 4;
			}
			// Use OR to avoid sign issues.
			key[byteNum - 1 - (i / 2)] |= (byte) nibble;
		}
		return key;
	}
	
	public static String bytesToHex(byte[] bArr) {
		String result = "";
		for (byte b : bArr) {
			result += String.format("%02x", b);
		}
		return result;
	}
	
	public static String strToHexStr(String str) {
		return String.format("%x", new BigInteger(1, str.getBytes()));
	}
	
}
