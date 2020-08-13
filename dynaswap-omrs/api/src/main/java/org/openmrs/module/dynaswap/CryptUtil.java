package org.openmrs.module.dynaswap;

import org.openmrs.util.Security;

public class CryptUtil {
	
	public CryptUtil() {
		
	}
	
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
	
	public static void main(String args[]) {
		
	}
}
