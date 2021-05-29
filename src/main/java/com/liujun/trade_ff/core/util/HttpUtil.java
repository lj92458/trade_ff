package com.liujun.trade_ff.core.util;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class HttpUtil {

	private static HttpUtil instance = new HttpUtil();
	private CloseableHttpClient client;

	public HttpUtil() {
		try {
			//client = HttpClients.createDefault();
			trustAllHosts();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void trustAllHosts() throws Exception {
		//SSLSocketFactory.getSocketFactory().setHostnameVerifier(new AllowAllHostnameVerifier());
		SSLContext sslContext = SSLContexts.custom().useTLS().build();
		sslContext.init(null, new TrustManager[] { new X509TrustManager() {
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(X509Certificate[] certs, String authType) {
			}

			public void checkServerTrusted(X509Certificate[] certs, String authType) {
			}

		} }, new SecureRandom());

		
		Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create().register("http", PlainConnectionSocketFactory.INSTANCE)
				.register("https", new SSLConnectionSocketFactory(sslContext, new AllowAllHostnameVerifier())).build();

		HttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
		client = HttpClients.custom().setConnectionManager(connManager).build();

	}

	public static HttpUtil getInstance() {
		return instance;
	}

	public CloseableHttpClient getHttpClient() {
		return client;
	}

	/**
	 * 用get方式发送请求,并返回应答内容(字符串形式)
	 * 
	 * @param url_prex
	 *            访问路径前半部分,不以“/”结尾
	 * @param url
	 *            访问路径后半部分,以“/”开头
	 * @param paramStr
	 *            请求参数拼接成的字符串,可为null
	 * @return 返回应答内容(字符串形式)
	 * @throws Exception
	 */
	public String requestHttpGet(String url_prex, String url, String paramStr) throws Exception {
		url = url_prex + url;
		if (StringUtil.notEmpty(paramStr)) {
			url = url + "?" + paramStr;
		}
		HttpGet httpGet = new HttpGet(url);
		CloseableHttpResponse response1 = client.execute(httpGet);
		HttpEntity entity1 = null;
		try {
			int httpState = response1.getStatusLine().getStatusCode();
			String reason = response1.getStatusLine().getReasonPhrase();
			entity1 = response1.getEntity();
			if (httpState >= 300) {
				throw new Exception("http状态异常:" + httpState + reason);
			}
			if (entity1 == null) {
				throw new Exception("应答消息为null");
			}
			String responseStr = EntityUtils.toString(entity1);
			return responseStr;
		} finally {
			// 将返回数据消费干净
			EntityUtils.consume(entity1);
			response1.close();
		}
	}

	/**
	 * 用post方式发送请求,并返回应答内容(字符串形式)
	 * 
	 * @param url_prex
	 *            访问路径前半部分,不以“/”结尾
	 * @param url
	 *            访问路径后半部分,以“/”开头
	 * @param paramMap
	 *            请求参数
	 * @return 返回应答内容(字符串形式)
	 * @throws Exception
	 */
	public String requestHttpPost(String url_prex, String url, Map<String, String> paramMap) throws Exception {
		url = url_prex + url;
		HttpPost httpPost = new HttpPost(url);
		List<NameValuePair> pairList = convertMap2PostParams(paramMap);
		httpPost.setEntity(new UrlEncodedFormEntity(pairList));
		CloseableHttpResponse response1 = client.execute(httpPost);
		HttpEntity entity1 = null;
		try {
			int httpState = response1.getStatusLine().getStatusCode();
			String reason = response1.getStatusLine().getReasonPhrase();
			entity1 = response1.getEntity();
			if (httpState >= 300) {
				throw new Exception("http状态异常:" + httpState + reason);
			}
			if (entity1 == null) {
				throw new Exception("应答消息为null");
			}
			String responseStr = EntityUtils.toString(entity1);
			return responseStr;

		} finally {
			// 将返回数据消费干净
			EntityUtils.consume(entity1);
			response1.close();
		}

	}


	private List<NameValuePair> convertMap2PostParams(Map<String, String> params) {
		List<String> keys = new ArrayList<String>(params.keySet());
		if (keys.isEmpty()) {
			return null;
		}
		int keySize = keys.size();
		List<NameValuePair> data = new LinkedList<NameValuePair>();
		for (int i = 0; i < keySize; i++) {
			String key = keys.get(i);
			String value = params.get(key);
			data.add(new BasicNameValuePair(key, value));
		}
		return data;
	}
}
