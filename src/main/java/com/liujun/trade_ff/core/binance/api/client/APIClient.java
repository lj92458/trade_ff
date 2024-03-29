package com.liujun.trade_ff.core.binance.api.client;


import com.alibaba.fastjson.JSON;
import com.liujun.trade_ff.core.binance.api.bean.ett.result.CursorPager;
import com.liujun.trade_ff.core.binance.api.bean.futures.HttpResult;
import com.liujun.trade_ff.core.binance.api.config.APIConfiguration;
import com.liujun.trade_ff.core.binance.api.constant.APIConstants;
import com.liujun.trade_ff.core.binance.api.exception.APIException;
import com.liujun.trade_ff.core.binance.api.utils.DateUtils;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * OKEX API Client
 *
 * @author Tony Tian
 * @version 1.0.0
 * @date 2018/3/8 13:43
 */
public class APIClient {

    private static final Logger LOG = LoggerFactory.getLogger(com.liujun.trade_ff.core.binance.api.client.APIClient.class);

    private final APIConfiguration config;
    private final APICredentials credentials;
    private final OkHttpClient client;
    private final Retrofit retrofit;
    private final ApiHttp apiHttp;

    /**
     * Initialize the apis client
     */
    public APIClient(final APIConfiguration config) {
        if (config == null || StringUtils.isEmpty(config.getEndpoint())) {
            throw new RuntimeException("The APIClient params can't be empty.");
        }
        this.config = config;
        this.credentials = new APICredentials(config);
        this.client = new APIHttpClient(config, this.credentials).client();
        this.retrofit = new APIRetrofit(config, this.client).retrofit();
        this.apiHttp = new ApiHttp(config, this.client);
    }

    /**
     * Initialize the retrofit operation service
     */
    public <T> T createService(final Class<T> service) {
        return this.retrofit.create(service);
    }

    public ApiHttp getApiHttp() {
        return this.apiHttp;
    }

    /**
     * Synchronous send request
     */
    //解析
    public <T> T executeSync(final Call<T> call) {
        try {

            final Response<T> response = call.execute();
            //System.out.println("response-------------------------"+call.toString());
            //是否打印config配置信息
            if (this.config.isPrint()) {
                //打印响应信息
                this.printResponse(response);
            }
            //获取状态码
            final int status = response.code();
            //获取错误信息
            final String message = new StringBuilder().append(response.code()).append(" / ").append(response.message()).toString();
            //响应成功
            if (response.isSuccessful()) {
                return response.body();
                ////如果状态码是403,429,418,504中的任意一个，抛出异常
            } else if (APIConstants.resultStatusArray.contains(status)) {
                final HttpResult result = JSON.parseObject(new String(response.errorBody().bytes()), HttpResult.class);
                if (result.getCode() == 0 && result.getMessage() == null && result.getMsg() == null) {
                    // System.out.println("错误码："+result.getErrorCode()+"\t错误信息"+result.getErrorMessage());
                    LOG.error(result.toString());
                    throw new APIException(result.getErrorCode(), result.getErrorMessage());
                } else {
                    //System.out.println("错误码："+result.getCode()+"\t错误信息"+result.getMessage());
                    //抛出异常
                    LOG.error(result.toString());
                    String msgStr = "";
                    if (result.getMessage() != null) {
                        msgStr += result.getMessage();
                    }
                    msgStr += ",";
                    if (result.getMsg() != null) {
                        msgStr += result.getMsg();
                    }
                    throw new APIException(result.getCode(), msgStr);
                }
            } else {
                throw new APIException(message + "\n" + response.body());
            }
        } catch (final IOException e) {
            throw new APIException("APIClient executeSync exception.", e);
        }
    }

    /**
     * Synchronous send request
     */
    public <T> CursorPager<T> executeSyncCursorPager(final Call<List<T>> call) {
        try {
            final Response<List<T>> response = call.execute();
            System.out.println("输出响应before");
            if (this.config.isPrint()) {
                this.printResponse(response);
            }
            System.out.println("输出响应after");
            final int status = response.code();
            final String message = response.code() + " / " + response.message();
            if (response.isSuccessful()) {
                final Headers headers = response.headers();
                final CursorPager<T> cursorPager = new CursorPager<T>();
                cursorPager.setData(response.body());
                cursorPager.setBefore(headers.get("OK-BEFORE"));
                cursorPager.setAfter(headers.get("OK-AFTER"));
                cursorPager.setLimit(Optional.ofNullable(headers.get("OK-LIMIT")).map(Integer::valueOf).orElse(100));
                return cursorPager;
            }
            if (APIConstants.resultStatusArray.contains(status)) {
                final HttpResult result = JSON.parseObject(new String(response.errorBody().bytes()), HttpResult.class);
                throw new APIException(result.getCode(), result.getMessage());
            }
            throw new APIException(message);
        } catch (final IOException e) {
            System.out.println("异常信息");
            throw new APIException("APIClient executeSync exception.", e);
        }
    }

    //输出响应（请求头，状态码，信息以及ResponseBody）
    private void printResponse(final Response response) {
        final StringBuilder responseInfo = new StringBuilder();
        responseInfo.append("\n\tResponse").append("(").append(DateUtils.timeToString(null, 4)).append("):");
        if (response != null) {

            responseInfo.append("\n\t\t").append("返回数据: ").append(response.toString());
            responseInfo.append("\n\t\t").append("Status: ").append(response.code());
            responseInfo.append("\n\t\t").append("Message: ").append(response.message());
            if (response.body() != null) {
                responseInfo.append("\n\t\t").append("Response Body: ").append(JSON.toJSONString(response.body()));
            }
        } else {
            responseInfo.append("\n\t\t").append("\n\tRequest Error: response is null");
        }
        com.liujun.trade_ff.core.binance.api.client.APIClient.LOG.info(responseInfo.toString());
    }

    @Override
    public String toString() {
        return "APIClient{" +
                "config=" + config +
                ", credentials=" + credentials +
                ", client=" + client +
                ", retrofit=" + retrofit +
                ", apiHttp=" + apiHttp +
                '}';
    }
}
