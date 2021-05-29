package com.liujun.trade_ff.core.bitfinex;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Map;

public class SignUtil {
	private static final Logger log = LoggerFactory.getLogger(SignUtil.class);
	private String accessKey;
	private String secretKey;
	private static TrustManager[] _trustAllCerts = null;
	private static HostnameVerifier _allHostsValid = null;
	private static final String HMAC_SHA1_ALGORITHM = "HmacSHA384";
	static {
		installAllCertsTruster();
	}

	public SignUtil(String accessKey, String secretKey) {
		this.accessKey = accessKey;
		this.secretKey = secretKey;
	}

	public String getNextSeq() {
		long val = System.currentTimeMillis() / 100L - 14330625078L;
		log.debug("btce获取的nonce:" + val);
		return val + "";
	}

	public String getSignature(String data, String key) throws Exception {

		// get an hmac_sha1 key from the raw key bytes
		SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), HMAC_SHA1_ALGORITHM);

		// get an hmac_sha1 Mac instance and initialize with the signing key
		Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
		mac.init(signingKey);

		// compute the hmac on input data bytes
		byte[] rawHmac = mac.doFinal(data.getBytes());

		return bytArrayToHex(rawHmac);
	}

	private String bytArrayToHex(byte[] a) {
		StringBuilder sb = new StringBuilder();
		for (byte b : a)
			sb.append(String.format("%02x", b & 0xff));
		return sb.toString();
	}

	/**
	 * post请求
	 * 
	 * @param url
	 *            完整的url
	 * @param method
	 *            请求的功能
	 * @param paramArr
	 * @return
	 * @throws Exception
	 */
	public String request(String url, String method, Map<String, Object> paramMap) throws Exception {
		StringBuilder jsonStrBuilder = new StringBuilder("{\"request\":\"").append(method).append("\"");
		jsonStrBuilder.append(",\"nonce\":\"").append(getNextSeq()).append("\"");
		for (String key : paramMap.keySet()) {
			String str = "";
			if (paramMap.get(key) instanceof String) {
				str = "\"";
			}
			jsonStrBuilder.append(",\"").append(key).append("\":").append(str).append(paramMap.get(key)).append(str);
		}
		jsonStrBuilder.append("}");
		//
		String paramsStr = jsonStrBuilder.toString();
		log.debug("paramsStr:" + paramsStr);
		//base64编码
		String payload_base64Str = new String(Base64.encodeBase64(paramsStr.getBytes("utf-8")));
		String sign_hexStr = getSignature(payload_base64Str, secretKey);
		log.debug("payload_base64Str:" + payload_base64Str);
		log.debug("sign_hexStr:" + sign_hexStr);
		URL obj = new URL(url);
		HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

		//add reuqest header
		con.setRequestMethod("POST");
		con.setRequestProperty("X-BFX-APIKEY", accessKey);
		con.setRequestProperty("X-BFX-PAYLOAD", payload_base64Str);
		con.setRequestProperty("X-BFX-SIGNATURE", sign_hexStr);

		// Send post request
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes("");
		wr.flush();
		wr.close();

		int responseCode = con.getResponseCode();

		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}

		in.close();
		if (responseCode != 200) {
			throw new Exception("http状态码错误:" + responseCode + ". response:" + response.toString());
		}
		return response.toString();

	}
	
	
	public String requestGet(String url, String paramStr) throws Exception {
		URL obj = new URL(url+"?"+paramStr);
		HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
		con.setRequestMethod("GET");
		int responseCode = con.getResponseCode();
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}

		in.close();
		if (responseCode != 200) {
			throw new Exception("http状态码错误:" + responseCode + ". response:" + response.toString());
		}
		
		return response.toString();
	}
	
	

	private static void installAllCertsTruster() {

		_trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(X509Certificate[] certs, String authType) {
			}

			public void checkServerTrusted(X509Certificate[] certs, String authType) {
			}

		} };

		try {
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, _trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (KeyManagementException kme) {
			System.err.println("Can't get key in SSL fix installer: " + kme.toString());
			System.exit(1);
		} catch (NoSuchAlgorithmException nsae) {
			System.err.println("Can't get algorithm in SSL fix installer: " + nsae.toString());
			System.exit(1);
		}

		// Create all-trusting host name verifier
		_allHostsValid = new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};

		// Install the all-trusting host verifier
		HttpsURLConnection.setDefaultHostnameVerifier(_allHostsValid);
	}
}
