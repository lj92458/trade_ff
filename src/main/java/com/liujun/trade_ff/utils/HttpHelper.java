package com.liujun.trade_ff.utils;

/**
 * HTTP协议帮助类
 * Created by WuShaotong on 2016/6/28.
 */
import org.apache.commons.io.IOUtils;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpHelper {
    private static PoolingHttpClientConnectionManager connMgr;
    private static RequestConfig requestConfig;
    private static final int CONN_TIMEOUT = 3000;
    private static final int READ_TIMEOUT = 7000;

    static {
        // 设置连接池
        connMgr = new PoolingHttpClientConnectionManager();
        // 设置连接池大小
        connMgr.setMaxTotal(100);
        connMgr.setDefaultMaxPerRoute(connMgr.getMaxTotal());

        RequestConfig.Builder configBuilder = RequestConfig.custom();
        // 设置连接超时
        configBuilder.setConnectTimeout(CONN_TIMEOUT);
        // 设置读取超时
        configBuilder.setSocketTimeout(READ_TIMEOUT);
        // 设置从连接池获取连接实例的超时
        configBuilder.setConnectionRequestTimeout(CONN_TIMEOUT);
        // 在提交请求之前 测试连接是否可用
        configBuilder.setStaleConnectionCheckEnabled(true);
        requestConfig = configBuilder.build();
    }

    /**
     * 发送 GET 请求（HTTP），不带输入数据
     * @param url
     * @return
     */
    public static String doGet(String url) {
        return doGet(url, new HashMap<String, Object>());
    }

    /**
     * 发送 GET 请求（HTTP），不带输入数据
     * @param url
     * @return
     */
    public static String doGet(String url, String encoding) {
        return doGet(url, new HashMap<String, Object>(), encoding);
    }

    /**
     * 发送 GET 请求（HTTP），K-V形式
     * @param url
     * @param params
     * @return
     */
    public static String doGet(String url, Map<String, Object> params) {
        return doGet(url,params,"UTF-8");
    }

    /**
     * 发送 GET 请求（HTTP），K-V形式
     * @param url
     * @param params
     * @return
     */
    public static String doGet(String url, Map<String, Object> params, String encoding) {
        String apiUrl = url;
        StringBuffer param = new StringBuffer();
        int i = 0;
        for (String key : params.keySet()) {
            if (i == 0)
                param.append("?");
            else
                param.append("&");
            param.append(key).append("=").append(params.get(key));
            i++;
        }
        apiUrl += param;
        String result = null;
        HttpClient httpclient = new DefaultHttpClient();
        try {
            HttpGet httpGet = new HttpGet(apiUrl);
            httpGet.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, CONN_TIMEOUT);
            httpGet.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, READ_TIMEOUT);
            HttpResponse response = httpclient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();

            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream instream = entity.getContent();
                result = IOUtils.toString(instream, encoding);
            }
        } catch (IOException e) {

        }
        return result;
    }

    /**
     * 发送 GET 请求（HTTP），K-V形式
     * @param url 请求地址
     * @param params 参数（map形式）
     * @param headers 请求头
     * @return 结果
     */
    public static String doGet(String url, Map<String, Object> params, Map<String, String> headers, Map<String, String> cookies) {
        return doGet(url, params, headers, cookies, null, 0);
    }

    /**
     * 发送 GET 请求（HTTP），K-V形式
     * @param url 请求地址
     * @param params 参数（map形式）
     * @param headers 请求头
     * @param proxyHost 代理地址
     * @param proxyPort 代理端口
     * @return 结果
     */
    public static String doGet(String url, Map<String, Object> params, Map<String, String> headers, Map<String, String> cookies, String proxyHost, int proxyPort) {
        try {
            byte[] respByteArr = doGetByteArr(url, params, headers, cookies, proxyHost, proxyPort);
            if(null == respByteArr){
                return null;
            }
            return new String(respByteArr, "UTF-8");
        } catch (Exception e) {

        }
        return null;
    }

    /**
     * 发送 GET 请求（HTTP），K-V形式
     * @param url 请求地址
     * @param params 参数（map形式）
     * @param headers 请求头
     * @param cookies Cookie
     * @return 结果
     */
    public static byte[] doGetByteArr(String url, Map<String, Object> params, Map<String, String> headers, Map<String, String> cookies) {
        return doGetByteArr(url, params, headers, cookies, null, 0);
    }

    /**
     * 发送 GET 请求（HTTP），K-V形式
     * @param url 请求地址
     * @param params 参数（map形式）
     * @param headers 请求头
     * @param cookies Cookie
     * @param proxyHost 代理地址
     * @param proxyPort 代理端口
     * @return 结果
     */
    public static byte[] doGetByteArr(String url, Map<String, Object> params, Map<String, String> headers, Map<String, String> cookies, String proxyHost, int proxyPort) {
        String apiUrl = url;
        if (null != params){
            int i = 0;
            StringBuffer param = new StringBuffer();
            for (String key : params.keySet()) {
                if (i == 0)
                    param.append("?");
                else
                    param.append("&");
                param.append(key).append("=").append(params.get(key));
                i++;
            }
            apiUrl += param;
        }
        byte[] result = null;
        HttpClient httpclient = new DefaultHttpClient();
        try {
            HttpGet httpGet = new HttpGet(apiUrl);
            httpGet.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, CONN_TIMEOUT);
            httpGet.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, READ_TIMEOUT);

            if(null != proxyHost && !"".equals(proxyHost.trim()) && proxyPort > 0){
                HttpHost proxy = new HttpHost(proxyHost, proxyPort);
                httpGet.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
            }
            //设置请求头 - common
            if(null != headers){
                for(String headName : headers.keySet()){
                    httpGet.addHeader(headName, headers.get(headName));
                }
            }
            //设置请求头 - 伪造IP
            String randomIp = generateRandomIpAddr();
            httpGet.addHeader("X-Forwarded-For", randomIp);
            httpGet.addHeader("client_ip", randomIp);

            //设置请求头 - cookies
            if(null != cookies){
                StringBuilder inCooliesSB = new StringBuilder("");
                for(String cookieName : cookies.keySet()){
                    inCooliesSB.append(cookieName + "=" + cookies.get(cookieName) + "; ");
                }
                httpGet.addHeader("Cookie", inCooliesSB.toString());
            }
            HttpResponse response = httpclient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();

            //获取cookies
            Header[] respCookies = response.getHeaders("Set-Cookie");
            if(null != respCookies && respCookies.length > 0 && null != cookies){
                for(Header cookie : respCookies){
                    try {
                        String[] cookieStr = cookie.getValue().split("=");
                        cookies.put(cookieStr[0], cookieStr[1]);
                    }catch(Exception e){

                    }
                }
            }

            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream instream = entity.getContent();
                result = new byte[instream.available()];
                instream.read(result);
            }

        } catch (Exception e) {

        }
        return result;
    }

    /**
     * 发送 POST 请求（HTTP），不带输入数据
     * @param apiUrl
     * @return
     */
    public static String doPost(String apiUrl) {
        return doPost(apiUrl, new HashMap<String, Object>());
    }

    /**
     * 发送 POST 请求（HTTP），K-V形式
     * @param apiUrl API接口URL
     * @param params 参数map
     * @return
     */
    public static String doPost(String apiUrl, Map<String, Object> params) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String httpStr = null;
        HttpPost httpPost = new HttpPost(apiUrl);
        CloseableHttpResponse response = null;

        try {
            httpPost.setConfig(requestConfig);
            List<NameValuePair> pairList = new ArrayList<NameValuePair>(params.size());
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                NameValuePair pair = new BasicNameValuePair(entry.getKey(), entry
                        .getValue().toString());
                pairList.add(pair);
            }
            httpPost.setEntity(new UrlEncodedFormEntity(pairList, Charset.forName("UTF-8")));
            response = httpClient.execute(httpPost);
            System.out.println(response.toString());
            HttpEntity entity = response.getEntity();
            httpStr = EntityUtils.toString(entity, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (response != null) {
                try {
                    EntityUtils.consume(response.getEntity());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return httpStr;
    }

    /**
     * 发送 POST 请求（HTTP），JSON形式
     * @param apiUrl
     * @param json json对象
     * @return
     */
    public static String doPost(String apiUrl, Object json) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String httpStr = null;
        HttpPost httpPost = new HttpPost(apiUrl);
        CloseableHttpResponse response = null;

        try {
            httpPost.setConfig(requestConfig);
            StringEntity stringEntity = new StringEntity(json.toString(),"UTF-8");//解决中文乱码问题
            stringEntity.setContentEncoding("UTF-8");
            stringEntity.setContentType("application/json");
            httpPost.setEntity(stringEntity);
            response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();
            System.out.println(response.getStatusLine().getStatusCode());
            httpStr = EntityUtils.toString(entity, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (response != null) {
                try {
                    EntityUtils.consume(response.getEntity());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return httpStr;
    }

    /**
     * 发送 SSL POST 请求（HTTPS），K-V形式
     * @param apiUrl API接口URL
     * @param params 参数map
     * @return
     */
    public static String doPostSSL(String apiUrl, Map<String, Object> params) {
        CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(createSSLConnSocketFactory()).setConnectionManager(connMgr).setDefaultRequestConfig(requestConfig).build();
        HttpPost httpPost = new HttpPost(apiUrl);
        CloseableHttpResponse response = null;
        String httpStr = null;

        try {
            httpPost.setConfig(requestConfig);
            List<NameValuePair> pairList = new ArrayList<NameValuePair>(params.size());
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                NameValuePair pair = new BasicNameValuePair(entry.getKey(), entry
                        .getValue().toString());
                pairList.add(pair);
            }
            httpPost.setEntity(new UrlEncodedFormEntity(pairList, Charset.forName("utf-8")));
            response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                return null;
            }
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                return null;
            }
            httpStr = EntityUtils.toString(entity, "utf-8");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (response != null) {
                try {
                    EntityUtils.consume(response.getEntity());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return httpStr;
    }


    public static String doPostSSL(String url, Map<String, Object> params, Map<String, String> headers, Map<String, String> cookies) {
        CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(createSSLConnSocketFactory()).setConnectionManager(connMgr).setDefaultRequestConfig(requestConfig).build();
        HttpPost httpPost = new HttpPost(url);
        CloseableHttpResponse response = null;
        String httpStr = null;

        try {
            httpPost.setConfig(requestConfig);
            List<NameValuePair> pairList = new ArrayList<NameValuePair>(params.size());
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                NameValuePair pair = new BasicNameValuePair(entry.getKey(), entry
                        .getValue().toString());
                pairList.add(pair);
            }
            httpPost.setEntity(new UrlEncodedFormEntity(pairList, Charset.forName("utf-8")));

            //设置请求头 - common
            if(null != headers){
                for(String headName : headers.keySet()){
                    httpPost.addHeader(headName, headers.get(headName));
                }
            }

            //设置请求头 - cookies
            if(null != cookies){
                StringBuilder inCooliesSB = new StringBuilder("");
                for(String cookieName : cookies.keySet()){
                    inCooliesSB.append(cookieName + "=" + cookies.get(cookieName) + "; ");
                }
                httpPost.addHeader("Cookie", inCooliesSB.toString());
            }

            response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                return null;
            }

            //获取cookies
            Header[] respCookies = response.getHeaders("Set-Cookie");
            if(null != respCookies && respCookies.length > 0 && null != cookies){
                for(Header cookie : respCookies){
                    try {
                        String[] cookieStr = cookie.getValue().split("=");
                        cookies.put(cookieStr[0], cookieStr[1]);
                    }catch(Exception e){

                    }
                }
            }

            HttpEntity entity = response.getEntity();
            if (entity == null) {
                return null;
            }
            httpStr = EntityUtils.toString(entity, "utf-8");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (response != null) {
                try {
                    EntityUtils.consume(response.getEntity());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return httpStr;
    }

    /**
     * 发送 SSL POST 请求（HTTPS），JSON形式
     * @param apiUrl API接口URL
     * @param json JSON对象
     * @return
     */
    public static String doPostSSL(String apiUrl, Object json) {
        CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(createSSLConnSocketFactory()).setConnectionManager(connMgr).setDefaultRequestConfig(requestConfig).build();
        HttpPost httpPost = new HttpPost(apiUrl);
        CloseableHttpResponse response = null;
        String httpStr = null;

        try {
            httpPost.setConfig(requestConfig);
            StringEntity stringEntity = new StringEntity(json.toString(),"UTF-8");//解决中文乱码问题
            stringEntity.setContentEncoding("UTF-8");
            stringEntity.setContentType("application/json");
            httpPost.setEntity(stringEntity);
            response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                return null;
            }
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                return null;
            }
            httpStr = EntityUtils.toString(entity, "utf-8");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (response != null) {
                try {
                    EntityUtils.consume(response.getEntity());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return httpStr;
    }

    /**
     * 创建SSL安全连接
     *
     * @return
     */
    private static SSLConnectionSocketFactory createSSLConnSocketFactory() {
        SSLConnectionSocketFactory sslsf = null;
        try {
            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {

                public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    return true;
                }
            }).build();
            sslsf = new SSLConnectionSocketFactory(sslContext, new X509HostnameVerifier() {

                @Override
                public boolean verify(String arg0, SSLSession arg1) {
                    return true;
                }

                @Override
                public void verify(String host, SSLSocket ssl) throws IOException {
                }

                @Override
                public void verify(String host, X509Certificate cert) throws SSLException {
                }

                @Override
                public void verify(String host, String[] cns, String[] subjectAlts) throws SSLException {
                }
            });
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
        return sslsf;
    }

    /**
     * 生成随机IP地址
     * @return 随机IP地址
     */
    private static String generateRandomIpAddr(){
        //美国硅谷 64.68.120.0 - 64.68.120.255 | 172.172.168.0 - 172.172.168.255
        return "172.172.168." + ((int) Math.floor(255 * Math.random()));
    }

    /**
     * 测试方法
     * @param args
     */
    public static void main(String[] args) throws Exception {

        String resp = HttpHelper.doGet("https://weixin110.qq.com/security/readtemplate?t=account_frozen/acct&type=unfrozen");
        System.out.println(resp);

    }
}