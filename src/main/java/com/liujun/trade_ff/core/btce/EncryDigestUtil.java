package com.liujun.trade_ff.core.btce;

import org.apache.commons.codec.binary.Hex;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;

final public class EncryDigestUtil {
	private StringCrypter userEncryptorKeys;
	private Mac mac;
	private MessageDigest degest;
	private String apiKey;

	public EncryDigestUtil(String aKey, String aSecret) {
		try {
			mac = Mac.getInstance("HMACSHA512");
			userEncryptorKeys = new StringCrypter();
			degest = MessageDigest.getInstance("SHA-1");
			setKeys(aKey, aSecret);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/** 设置密钥 */
	public synchronized void setKeys(String aKey, String aSecret) throws Exception {
		if (aKey.length() > 0 && aSecret.length() > 0) {
			apiKey = aKey;
			SecretKeySpec key = new SecretKeySpec(aSecret.getBytes("UTF-8"), "HmacSHA512");
			mac.init(key);
		} else {
			throw new Exception("Invalid key or secret");
		}
	}

	/** 设置加密后的密钥 */
	public synchronized void setKeys(String encodedKey, String encodedSecret, String decodeKey) throws Exception {
		userEncryptorKeys.setKey(hashStr(decodeKey));
		apiKey = userEncryptorKeys.decrypt(encodedKey);
		SecretKeySpec key = new SecretKeySpec(userEncryptorKeys.decrypt(encodedSecret).getBytes("UTF-8"), "HmacSHA512");
		mac.init(key);
	}

	/** 签名 */
	public String sign(String dataStr) {

		try {
			String resultStr = new String(Hex.encodeHex(mac.doFinal(dataStr.getBytes("UTF-8"))));
			return resultStr;
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}

	}

	/**
	 * Encode string to DESede with SHA-1 from 'key' (key->SHA-1->DESede)
	 * 
	 * @param sourceStr
	 *            original string
	 * @param userKey
	 *            key for encoding
	 * @return encoded string
	 * @throws Exception
	 */
	public synchronized String encodeStr(String sourceStr, String userKey) throws Exception {
		userEncryptorKeys.setKey(hashStr(userKey));
		return userEncryptorKeys.encrypt(sourceStr);
	}

	/**
	 * Decode string from DESede
	 * 
	 * @param encodeStr
	 *            encoded string
	 * @param userKey
	 *            key for decoding
	 * @return decoded string
	 * @throws Exception
	 */
	public synchronized String decodeStr(String encodeStr, String userKey) throws Exception {
		userEncryptorKeys.setKey(hashStr(userKey));
		return userEncryptorKeys.decrypt(encodeStr);
	}

	private String hashStr(String str) throws Exception {
		StringBuilder hashCode = new StringBuilder();
		byte[] digest = degest.digest(str.getBytes());
		for (int i = 0; i < digest.length; i++) {
			hashCode.append(Integer.toHexString(0x0100 + (digest[i] & 0x00FF)).substring(1));
		}
		return hashCode.toString();
	}

}
