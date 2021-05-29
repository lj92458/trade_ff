package com.liujun.trade_ff.core.btce;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import java.security.spec.KeySpec;

final class StringCrypter {
	private static final String UNICODE_FORMAT = "UTF8";
	private static final String DESEDE_ENCRYPTION_SCHEME = "DESede";
	private KeySpec myKeySpec;
	private SecretKeyFactory mySecretKeyFactory;
	private Cipher cipher;
	private byte[] keyAsBytes;
	private String myEncryptionKey;
	private String myEncryptionScheme;
	private SecretKey key;

	void setKey(String inputEncryptionKey) throws Exception {
		myEncryptionKey = inputEncryptionKey;
		myEncryptionScheme = DESEDE_ENCRYPTION_SCHEME;
		keyAsBytes = myEncryptionKey.getBytes(UNICODE_FORMAT);
		myKeySpec = new DESedeKeySpec(keyAsBytes);
		mySecretKeyFactory = SecretKeyFactory.getInstance(myEncryptionScheme);
		cipher = Cipher.getInstance(myEncryptionScheme);
		key = mySecretKeyFactory.generateSecret(myKeySpec);
	}

	String encrypt(String unencryptedString) throws Exception {
		cipher.init(Cipher.ENCRYPT_MODE, key);
		byte[] plainText = unencryptedString.getBytes(UNICODE_FORMAT);
		byte[] encryptedText = cipher.doFinal(plainText);
		String encryptedString = new String(Base64.encodeBase64(encryptedText));
		return encryptedString;
	}

	String decrypt(String encryptedString) throws Exception {
		cipher.init(Cipher.DECRYPT_MODE, key);
		byte[] encryptedText = Base64.decodeBase64(encryptedString.getBytes());
		byte[] plainText = cipher.doFinal(encryptedText);
		String decryptedText = bytes2String(plainText);
		return decryptedText;
	}

	private static String bytes2String(byte[] bytes) {
		StringBuffer stringBuffer = new StringBuffer();
		for (int i = 0; i < bytes.length; i++) {
			stringBuffer.append((char) bytes[i]);
		}
		return stringBuffer.toString();
	}
}
