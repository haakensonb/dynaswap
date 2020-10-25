package org.openmrs.module.dynaswap;

import org.junit.Test;
import org.openmrs.module.dynaswap.atallah.CryptUtil;
import org.openmrs.util.Security;
import static org.junit.Assert.*;

import java.util.Arrays;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * This is a unit test, which verifies logic in DynaSWAPBaseModuleService. It doesn't extend
 * BaseModuleContextSensitiveTest, thus it is run without the in-memory DB and Spring context.
 */
public class CryptUtilTest {
	
	@Test
	public void hashFunc_shouldConcateOption() {
		String val1 = Security.getRandomToken();
		String val2 = Security.getRandomToken();
		String valOpt = "0";
		String hash1 = CryptUtil.hashFunc(val1, val2);
		String hash2 = CryptUtil.hashFunc(val1, val2, valOpt);
		boolean hashEqual = hash1.equals(hash2);
		assertFalse(hashEqual);
	}
	
	@Test
	public void encrypt_decrypt() {
		String str1 = "hello";
		String str2 = "world";
		String keyHexStr = DigestUtils.sha256Hex("myKey");
		String ciphertext = CryptUtil.encrypt(keyHexStr, str1, str2);
		String plaintext = CryptUtil.decrypt(ciphertext, keyHexStr);
		final String expectedPlaintext = str1 + str2;
		assertEquals(expectedPlaintext, plaintext);
	}
	
	@Test
	public void hexStrToByteArrayAndBack() {
		// String hexStr1 = "21fc9a4ee52169b0613f591bfd2cb8b6";
		// byte[] bArr1 = CryptUtil.hexStringToByteArray(hexStr1);
		// String newHexStr1 = CryptUtil.bytesToHex(bArr1);
		// assertEquals(hexStr1, newHexStr1);
		
		// String hexStr2 = "262f8551fe6953966d0df3b56f5f6074";
		// byte[] bArr2 = CryptUtil.hexStringToByteArray(hexStr2);
		// String newHexStr2 = CryptUtil.bytesToHex(bArr2);
		// assertEquals(hexStr2, newHexStr2);
		
		// String hexStr3 = "c4e19ad7ff640c5eff0facd4333216e9ed70e395e8a0465989eeb25986297beb";
		// byte[] bArr3 = CryptUtil.hexStringToByteArray(hexStr3);
		// System.out.println("bArr3: " + bArr3);
		// String newHexStr3 = CryptUtil.bytesToHex(bArr3);
		// assertEquals(hexStr3, newHexStr3);
		
		String str = "Nc2GcGbo7aNbaHP+0VeZk59zOzwCitBXPc4En1mXvHdVWjpNvIeqgL76ipL86IMvB0DUYf4LkLu1I4VwzHT7/6Avvu3+56odBAEWvS27oa1YN4qmIzjAd+zZKDC8tYucGYIHrtixRcre0DzGQtX2kjQR9uew/RLigEhK4R2565+LegqElbLrTRoTRUu61oCy";
		String strHex = CryptUtil.strToHexStr(str);
		System.out.println("strHex: " + strHex);
		
	}
}
