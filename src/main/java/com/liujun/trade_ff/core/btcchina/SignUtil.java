package com.liujun.trade_ff.core.btcchina;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.*;
import javax.xml.bind.DatatypeConverter;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

public class SignUtil {
	private static final Logger log = LoggerFactory.getLogger(SignUtil.class);
	private String accessKey;
	private String secretKey;
	private static TrustManager[] _trustAllCerts = null;
	private static HostnameVerifier _allHostsValid = null;
	private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
	static {
		installAllCertsTruster();
	}

	public SignUtil(String accessKey, String secretKey) {
		this.accessKey = accessKey;
		this.secretKey = secretKey;
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

	public String request(String url, String method, Object[] paramArr) throws Exception {
		StringBuilder paramBuilder = new StringBuilder();
		for (Object o : paramArr) {
			if (paramBuilder.length() != 0) {
				paramBuilder.append(',');
			}
			if (o instanceof String) {
				paramBuilder.append('"').append(o.toString()).append('"');
			} else {

				paramBuilder.append(o.toString());
			}
		}

		String paramsStr = paramBuilder.toString();
		log.debug("paramsStr:" + paramsStr);
		//拼接签名串时，要去掉引号，将true替换为1，false替换为空
		String signParam = paramsStr.replace("\"", "");
		//signParam = signParam.replace("true", "1");
		//signParam = signParam.replace("false", "");
		log.debug("signParam:" + signParam);
		//
		String tonce = "" + (System.currentTimeMillis() * 1000);
		String params = "tonce=" + tonce + "&accesskey=" + accessKey + "&requestmethod=post&id=1&method=" + method + "&params=" + signParam;
		String hash = getSignature(params, secretKey);

		URL obj = new URL(url);
		HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
		String userpass = accessKey + ":" + hash;
		//String basicAuth = "Basic " + Base64.encodeBase64(userpass.getBytes());//
		String basicAuth = "Basic " + DatatypeConverter.printBase64Binary(userpass.getBytes());
		//add reuqest header
		con.setRequestMethod("POST");
		con.setRequestProperty("Json-Rpc-Tonce", tonce);
		con.setRequestProperty("Authorization", basicAuth);

		String postdata = "{\"method\": \"" + method + "\", \"params\": [" + paramsStr + "], \"id\": 1}";

		// Send post request
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(postdata);
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
		URL obj = new URL(url + "?" + paramStr);
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
