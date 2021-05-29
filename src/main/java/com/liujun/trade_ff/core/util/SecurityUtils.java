package com.liujun.trade_ff.core.util;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.Validate;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * @Description 安全工具类（含加解密、摘要算法）
 * @author caobin
 * @date 2012-11-15
 * @version 1.0
 */
public class SecurityUtils {

	private SecurityUtils() {

	};

	/**
	 * @Description 摘要功能
	 * @author caobin
	 * @date 2012-11-15
	 * @version 1.0
	 */
	public static class DigestFunc {

		/**
		 * 摘要算法
		 * 
		 * @param input
		 *            输入
		 * @param algorithm
		 *            摘要算法
		 * @return
		 */
		public static String digest2Hex(byte[] input, DigestAlgorithm algorithm) {
			return Hex.encodeHexString(digest(input, algorithm, null, 1));
		}

		/**
		 * 摘要算法
		 * 
		 * @param input
		 *            输入
		 * @param algorithm
		 *            摘要算法
		 * @return
		 */
		public static byte[] digest(byte[] input, DigestAlgorithm algorithm) {
			return digest(input, algorithm, null, 1);
		}

		/**
		 * 迭代摘要算法
		 * 
		 * @param input
		 *            输入
		 * @param algorithm
		 *            摘要算法
		 * @param salt
		 *            盐(不加盐处理则设置为null)
		 * @param iterations
		 *            迭代次数(1-2048，不迭代设置为1)
		 * @return
		 */
		public static String digest2Hex(byte[] input, DigestAlgorithm algorithm, byte[] salt, int iterations) {
			return Hex.encodeHexString(digest(input, algorithm, salt, iterations));

		}

		/**
		 * 迭代摘要算法
		 * 
		 * @param input
		 *            输入
		 * @param algorithm
		 *            摘要算法
		 * @param salt
		 *            盐(不加盐处理则设置为null)
		 * @param iterations
		 *            迭代次数(1-2048，不迭代设置为1)
		 * @return
		 */
		public static byte[] digest(byte[] input, DigestAlgorithm algorithm, byte[] salt, int iterations) {

			try {
				Validate.inclusiveBetween(1, 2048, iterations);
				MessageDigest digest = MessageDigest.getInstance(algorithm.getAlgorithm());
				if (salt != null) {
					digest.update(salt);
				}

				byte[] result = digest.digest(input);

				for (int i = 1; i < iterations; i++) {
					digest.reset();
					result = digest.digest(result);
				}
				return result;
			} catch (GeneralSecurityException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * @Description 加解密功能
	 * @author caobin
	 * @date 2012-11-15
	 * @version 1.0
	 */
	public static class CryptoFunc {

		private static final String AES = "AES";
		private static final String AES_CBC = "AES/CBC/PKCS5Padding";
		private static final String HMACSHA512 = "HmacSHA512";

		private static final int DEFAULT_HMACSHA512_KEYSIZE = 512; //RFC2401 ,sha1长度160，sha256长度256
		private static final int DEFAULT_AES_KEYSIZE = 128;
		private static final int DEFAULT_IVSIZE = 16;

		/**
		 * 定义DES向量
		 */
		private static final byte[] DES_IV = { 1, 2, 3, 4, 5, 6, 7, 8 };

		/**
		 * DES CBC
		 */
		private static final String DES_CBC = "DES/CBC/PKCS5Padding";
		/**
		 * DES ENCODE
		 */
		private static final String DES_ENCODE = "UTF-8";

		private static SecureRandom random = new SecureRandom();

		/**
		 * @Description DES加密
		 * @param encryptString
		 *            待加密字串
		 * @param encryptKey
		 *            加密密钥
		 * @return
		 * @throws Exception
		 * @author caobin
		 */
		public static String desEncrypt(String encryptString, String encryptKey) throws Exception {
			IvParameterSpec zeroIv = new IvParameterSpec(DES_IV);
			SecretKeySpec key = new SecretKeySpec(encryptKey.getBytes(DES_ENCODE), "DES");
			Cipher cipher = Cipher.getInstance(DES_CBC);
			cipher.init(Cipher.ENCRYPT_MODE, key, zeroIv);
			byte[] encryptedData = cipher.doFinal(encryptString.getBytes(DES_ENCODE));

			return new String(Base64.encodeBase64(encryptedData), DES_ENCODE);
		}

		/**
		 * @Description DES解密
		 * @param decryptString
		 *            待解密字串
		 * @param decryptKey
		 *            解密密钥
		 * @return
		 * @throws Exception
		 * @author caobin
		 */
		public static String desDecrypt(String decryptString, String decryptKey) throws Exception {
			byte[] byteMi = Base64.decodeBase64(decryptString.getBytes(DES_ENCODE));
			IvParameterSpec zeroIv = new IvParameterSpec(DES_IV);
			SecretKeySpec key = new SecretKeySpec(decryptKey.getBytes(DES_ENCODE), "DES");
			Cipher cipher = Cipher.getInstance(DES_CBC);
			cipher.init(Cipher.DECRYPT_MODE, key, zeroIv);
			byte decryptedData[] = cipher.doFinal(byteMi);

			return new String(decryptedData, DES_ENCODE);
		}

		//-- HMAC-SHA512 funciton --//
		/**
		 * 使用HMAC-SHA512进行消息签名, 返回字节数组,长度为20字节.
		 * 
		 * @param input
		 *            原始输入字符数组
		 * @param key
		 *            HMAC-SHA512密钥
		 */
		public static byte[] hmacSha512(byte[] input, byte[] key) {
			try {
				SecretKey secretKey = new SecretKeySpec(key, HMACSHA512);
				Mac mac = Mac.getInstance(HMACSHA512);
				mac.init(secretKey);
				return mac.doFinal(input);
			} catch (GeneralSecurityException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 校验HMAC-SHA512签名是否正确.
		 * 
		 * @param expected
		 *            已存在的签名
		 * @param input
		 *            原始输入字符串
		 * @param key
		 *            密钥
		 */
		public static boolean isMacValid(byte[] expected, byte[] input, byte[] key) {
			byte[] actual = hmacSha512(input, key);
			return Arrays.equals(expected, actual);
		}

		/**
		 * 生成HMAC-SHA512密钥,返回字节数组,长度为512位(64字节). HMAC-SHA512算法对密钥无特殊要求,
		 * RFC2401建议最少长度为
		 */
		public static byte[] generateHmacSha512Key() {
			try {
				KeyGenerator keyGenerator = KeyGenerator.getInstance(HMACSHA512);
				keyGenerator.init(DEFAULT_HMACSHA512_KEYSIZE);
				SecretKey secretKey = keyGenerator.generateKey();
				return secretKey.getEncoded();
			} catch (GeneralSecurityException e) {
				throw new RuntimeException(e);
			}
		}

		//-- AES funciton --//
		/**
		 * 使用AES加密原始字符串.
		 * 
		 * @param input
		 *            原始输入字符数组
		 * @param key
		 *            符合AES要求的密钥
		 */
		public static byte[] aesEncrypt(byte[] input, byte[] key) {
			return aes(input, key, Cipher.ENCRYPT_MODE);
		}

		/**
		 * 使用AES加密原始字符串.
		 * 
		 * @param input
		 *            原始输入字符数组
		 * @param key
		 *            符合AES要求的密钥
		 * @param iv
		 *            初始向量
		 */
		public static byte[] aesEncrypt(byte[] input, byte[] key, byte[] iv) {
			return aes(input, key, iv, Cipher.ENCRYPT_MODE);
		}

		/**
		 * 使用AES解密字符串, 返回原始字符串.
		 * 
		 * @param input
		 *            Hex编码的加密字符串
		 * @param key
		 *            符合AES要求的密钥
		 */
		public static String aesDecrypt(byte[] input, byte[] key) {
			byte[] decryptResult = aes(input, key, Cipher.DECRYPT_MODE);
			return new String(decryptResult);
		}

		/**
		 * 使用AES解密字符串, 返回原始字符串.
		 * 
		 * @param input
		 *            Hex编码的加密字符串
		 * @param key
		 *            符合AES要求的密钥
		 * @param iv
		 *            初始向量
		 */
		public static String aesDecrypt(byte[] input, byte[] key, byte[] iv) {
			byte[] decryptResult = aes(input, key, iv, Cipher.DECRYPT_MODE);
			return new String(decryptResult);
		}

		/**
		 * 使用AES加密或解密无编码的原始字节数组, 返回无编码的字节数组结果.
		 * 
		 * @param input
		 *            原始字节数组
		 * @param key
		 *            符合AES要求的密钥
		 * @param mode
		 *            Cipher.ENCRYPT_MODE 或 Cipher.DECRYPT_MODE
		 */
		private static byte[] aes(byte[] input, byte[] key, int mode) {
			try {
				SecretKey secretKey = new SecretKeySpec(key, AES);
				Cipher cipher = Cipher.getInstance(AES);
				cipher.init(mode, secretKey);
				return cipher.doFinal(input);
			} catch (GeneralSecurityException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 使用AES加密或解密无编码的原始字节数组, 返回无编码的字节数组结果.
		 * 
		 * @param input
		 *            原始字节数组
		 * @param key
		 *            符合AES要求的密钥
		 * @param iv
		 *            初始向量
		 * @param mode
		 *            Cipher.ENCRYPT_MODE 或 Cipher.DECRYPT_MODE
		 */
		private static byte[] aes(byte[] input, byte[] key, byte[] iv, int mode) {
			try {
				SecretKey secretKey = new SecretKeySpec(key, AES);
				IvParameterSpec ivSpec = new IvParameterSpec(iv);
				Cipher cipher = Cipher.getInstance(AES_CBC);
				cipher.init(mode, secretKey, ivSpec);
				return cipher.doFinal(input);
			} catch (GeneralSecurityException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 生成AES密钥,返回字节数组, 默认长度为128位(16字节).
		 */
		public static byte[] generateAesKey() {
			return generateAesKey(DEFAULT_AES_KEYSIZE);
		}

		/**
		 * 生成AES密钥,可选长度为128,192,256位.
		 */
		public static byte[] generateAesKey(int keysize) {
			try {
				KeyGenerator keyGenerator = KeyGenerator.getInstance(AES);
				keyGenerator.init(keysize);
				SecretKey secretKey = keyGenerator.generateKey();
				return secretKey.getEncoded();
			} catch (GeneralSecurityException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * 生成随机向量,默认大小为cipher.getBlockSize(), 16字节.
		 */
		public static byte[] generateIV() {
			byte[] bytes = new byte[DEFAULT_IVSIZE];
			random.nextBytes(bytes);
			return bytes;
		}

	}

	/**
	 * @Description 摘要算法枚举（部分算法需要BouncyCastle支持）
	 * @author caobin
	 * @date 2012-11-15
	 * @version 1.0
	 */
	public static enum DigestAlgorithm {
		MD5("MD5"), SHA("SHA"), TIGER("Tiger"), WHIRLPOOL("Whirlpool");

		private final String algorithm;

		DigestAlgorithm(String algorithm) {
			this.algorithm = algorithm;
		}

		public String getAlgorithm() {
			return algorithm;
		}

	}
	/*
	public static void main(String[] args) {
		//byte[] arr= CryptoFunc.generateHmacSha512Key();
		//System.out.println(Hex.encodeHexString(arr) );
		//byte[] key= "5c10415b4a67718c0f1a2ad26766f6c4beb5ae5243933192bd0dfbd0dd2c40e88647d455a1007005875ba4de57301875d274a47bf73a0d3ae82e400882bccdec".getBytes();
		byte[] key= "eb00bd6218552a809082b2ce58e15ecd77e73cd2412b723f0aab6008ef68c752".getBytes();
		byte[] msgArr= "method=getInfo&nonce=1935341".getBytes();
		byte[] sign = CryptoFunc.hmacSha512(msgArr, key);
		System.out.println("sign:"+Hex.encodeHexString(sign));//a07aab35121cb4154108161349ba7947ef228b8f7143caa53f7347653da3b8648a50dda6d24d2d7796258caa686ba2fec62db8c0fa0bc48283d24939375a126b
	//
		boolean bool = CryptoFunc.isMacValid(sign, msgArr, key);
		System.out.println(bool);
	}
	*/
	
	public static void main(String[] args) throws UnsupportedEncodingException {
		String str = URLEncoder.encode("method=getInfo&nonce=1935341", "utf-8");
		System.out.println(str);
	}
}
