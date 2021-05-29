package com.liujun.trade_ff.core.binance.api.client;

import com.liujun.trade_ff.core.binance.api.config.APIConfiguration;
import com.liujun.trade_ff.core.binance.api.constant.APIConstants;
import com.liujun.trade_ff.core.binance.api.enums.ContentTypeEnum;
import com.liujun.trade_ff.core.binance.api.enums.HttpHeadersEnum;
import com.liujun.trade_ff.core.binance.api.enums.SECURITY_TYPE;
import com.liujun.trade_ff.core.binance.api.exception.APIException;
import com.liujun.trade_ff.core.binance.api.utils.DateUtils;
import com.liujun.trade_ff.core.binance.api.utils.HmacSHA256Base64Utils;
import okhttp3.*;
import okio.Buffer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.util.concurrent.TimeUnit;

/**
 * API OkHttpClient.
 *
 * @author Tony Tian
 * @version 1.0.0
 * @date 2018/3/8 14:14
 */
public class APIHttpClient {

    private static final Logger LOG = LoggerFactory.getLogger(com.liujun.trade_ff.core.binance.api.client.APIHttpClient.class);

    private final APIConfiguration config;
    private final APICredentials credentials;

    public APIHttpClient(final APIConfiguration config, final APICredentials credentials) {
        this.config = config;
        this.credentials = credentials;
    }

    /**
     * Get a ok http 3 client object. <br/>
     * Declare:
     * <blockquote><pre>
     *  1. Set default client args:
     *         connectTimeout=30s
     *         readTimeout=30s
     *         writeTimeout=30s
     *         retryOnConnectionFailure=true.
     *  2. Set request headers:
     *      Content-Type: application/json; charset=UTF-8  (default)
     *      Cookie: locale=en_US        (English)
     *      OK-ACCESS-KEY: (Your setting)
     *      OK-ACCESS-SIGN: (Use your setting, auto sign and add)
     *      OK-ACCESS-TIMESTAMP: (Auto add)
     *      OK-ACCESS-PASSPHRASE: Your setting
     *  3. Set default print api info: false.
     * </pre></blockquote>
     */
    public OkHttpClient client() {
        final OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        // clientBuilder.proxy(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1",10810)));

        clientBuilder.connectTimeout(this.config.getConnectTimeout(), TimeUnit.SECONDS);
        clientBuilder.readTimeout(this.config.getReadTimeout(), TimeUnit.SECONDS);
        clientBuilder.writeTimeout(this.config.getWriteTimeout(), TimeUnit.SECONDS);
        clientBuilder.retryOnConnectionFailure(this.config.isRetryOnConnectionFailure());
        clientBuilder.addInterceptor((Interceptor.Chain chain) -> {
            Request oldRequest = chain.request();
            final Request.Builder requestBuilder = oldRequest.newBuilder();
            final String timestamp = DateUtils.getUnixTime();
            //打印首行时间戳
            //System.out.println("时间戳timestamp={" + timestamp + "}");
            requestBuilder.headers(this.headers(oldRequest, timestamp));
            //为request生成签名，然后拼接上签名
            this.addTimeAndSign(oldRequest, requestBuilder);
            final Request newRequest = requestBuilder.build();
            if (this.config.isPrint()) {
                this.printRequest(newRequest, timestamp, oldRequest);
            }
            return chain.proceed(newRequest);
        });
        return clientBuilder.build();
    }

    /**
     * 为request生成签名，然后拼接上签名
     *
     * @param oldRequest
     * @param requestBuilder
     */
    private void addTimeAndSign(Request oldRequest, Request.Builder requestBuilder) {
        String query = queryString(oldRequest);
        //如果请求头中有SECURITY_TYPE，并且需要签名

        String securityType = oldRequest.header(HttpHeadersEnum.SECURITY_TYPE.header());
        //根据接口安全类型，决定是否要签名
        if (StringUtils.isNotEmpty(this.credentials.getSecretKey()) && StringUtils.isNotEmpty(securityType)) {
            SECURITY_TYPE type = Enum.valueOf(SECURITY_TYPE.class, securityType);
            //TRADE, MARGIN 和USER_DATA需要签名
            if (APIConstants.NEED_SIGN_TYPE_LIST.contains(type)) {
                String signature;
                try {
                    signature = HmacSHA256Base64Utils.sign(query + body(oldRequest), this.credentials.getSecretKey());
                    //System.out.println("签名字符串："+timestamp+this.method(request)+this.requestPath(request)+this.queryString(request)+this.body(request));
                } catch (final IOException e) {
                    throw new APIException("Request get body io exception.", e);
                } catch (final CloneNotSupportedException e) {
                    throw new APIException("Hmac SHA256 Base64 Signature clone not supported exception.", e);
                } catch (final InvalidKeyException e) {
                    throw new APIException("Hmac SHA256 Base64 Signature invalid key exception.", e);
                }
                if (StringUtils.isNotEmpty(query)) {
                    query += "&";
                }
                query += "signature=" + signature;
            }
        }
        String newUrl = this.config.getEndpoint() + requestPath(oldRequest) + "?" + query;
        requestBuilder.url(newUrl);
    }

    private Headers headers(final Request oldRequest, final String timestamp) {
        final Headers.Builder builder = new Headers.Builder();
        builder.add(APIConstants.ACCEPT, ContentTypeEnum.APPLICATION_JSON.contentType());
        builder.add(APIConstants.CONTENT_TYPE, ContentTypeEnum.APPLICATION_FORM.contentType());
        //builder.add(APIConstants.COOKIE, this.getCookie());
        String securityType = oldRequest.header(HttpHeadersEnum.SECURITY_TYPE.header());
        //根据接口安全类型，决定是否要签名
        if (StringUtils.isNotEmpty(this.credentials.getSecretKey())
                && StringUtils.isNotEmpty(securityType)
                && Enum.valueOf(SECURITY_TYPE.class, securityType) != SECURITY_TYPE.NONE) {
            //添加公钥
            builder.add(HttpHeadersEnum.X_MBX_APIKEY.header(), this.credentials.getApiKey());

        }
        return builder.build();
    }

    private String getCookie() {
        final StringBuilder cookie = new StringBuilder();
        cookie.append(APIConstants.LOCALE).append(this.config.getI18n().i18n());
        return cookie.toString();
    }


    //返回请求路径url
    private String url(final Request request) {
        return request.url().toString();
    }

    //将请求方法转变为大写，并返回
    private String method(final Request request) {
        return request.method().toUpperCase();
    }

    //返回请求路径
    private String requestPath(final Request request) {
        String url = this.url(request);
        url = url.replace(this.config.getEndpoint(), APIConstants.EMPTY);
        String requestPath = url;
        if (requestPath.contains(APIConstants.QUESTION)) {
            requestPath = requestPath.substring(0, url.lastIndexOf(APIConstants.QUESTION));
        }
        if (this.config.getEndpoint().endsWith(APIConstants.SLASH)) {
            requestPath = APIConstants.SLASH + requestPath;
        }
        return requestPath;
    }

    private String queryString(final Request request) {
        final String url = this.url(request);
        //请求参数为空字符串
        String queryString = APIConstants.EMPTY;
        //如果URL中包含？即存在参数的拼接
        if (url.contains(APIConstants.QUESTION)) {
            queryString = url.substring(url.lastIndexOf(APIConstants.QUESTION) + 1);
        }
        return queryString;
    }

    private String body(final Request request) throws IOException {
        final RequestBody requestBody = request.body();
        String body = APIConstants.EMPTY;
        if (requestBody != null) {
            final Buffer buffer = new Buffer();
            requestBody.writeTo(buffer);
            body = buffer.readString(APIConstants.UTF_8);
        }
        return body;
    }

    private void printRequest(final Request newRequest, final String timestamp, Request oldRequest) {
        final String method = this.method(newRequest);
        final String requestPath = this.requestPath(newRequest);

        final String queryString = this.queryString(newRequest);

        final String body;
        try {
            body = this.body(newRequest);
        } catch (final IOException e) {
            throw new APIException("Request get body io exception.", e);
        }
        final StringBuilder requestInfo = new StringBuilder();


        requestInfo.append("\n\tRequest").append("(").append(DateUtils.timeToString(null, 4)).append("):");
        //拼接Url
        requestInfo.append("\n\t\t").append("Url: ").append(this.url(newRequest));
        requestInfo.append("\n\t\t").append("Method: ").append(method);
        requestInfo.append("\n\t\t").append("Headers: ");
        final Headers headers = newRequest.headers();
        if (headers != null && headers.size() > 0) {
            for (final String name : headers.names()) {
                requestInfo.append("\n\t\t\t").append(name).append(": ").append(headers.get(name));
            }
        }
        requestInfo.append("\n\t\t").append("request body: ").append(body);
        //final String preHash = HmacSHA256Base64Utils.preHash(timestamp, method, requestPath, queryString, body);
        try {

            requestInfo.append("\n\t\t").append("preHash: ").append(queryString(oldRequest) + body(oldRequest));
        } catch (Exception e) {
            throw new APIException("Request get body io exception.", e);
        }
        com.liujun.trade_ff.core.binance.api.client.APIHttpClient.LOG.info(requestInfo.toString());
    }
}
