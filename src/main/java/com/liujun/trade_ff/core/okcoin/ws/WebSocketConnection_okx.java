package com.liujun.trade_ff.core.okcoin.ws;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.binance.connector.client.utils.WebSocketConnection;
import com.binance.connector.client.utils.websocketcallback.*;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.okex.open.api.bean.other.OrderBookItem;
import com.okex.open.api.bean.other.SpotOrderBook;
import com.okex.open.api.bean.other.SpotOrderBookDiff;
import com.okex.open.api.bean.other.SpotOrderBookItem;
import com.okex.open.api.enums.CharsetEnum;
import com.okex.open.api.utils.DateUtils;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.joda.time.DateTime;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Getter
@Setter
public class WebSocketConnection_okx extends WebSocketConnection {
    public WebSocket webSocket; //无法访问父类的webSocket，所以只好找机会保存下来
    private ScheduledExecutorService service;
    private static Boolean flag = false;
    private static Boolean isConnect = false;
    private static String sign;
    private final static HashFunction crc32 = Hashing.crc32();
    public static final Map<String, Optional<SpotOrderBook>> bookMap = new HashMap<>();

    public WebSocketConnection_okx(WebSocketOpenCallback onOpenCallback, WebSocketMessageCallback onMessageCallback, WebSocketClosingCallback onClosingCallback, WebSocketClosedCallback onClosedCallback, WebSocketFailureCallback onFailureCallback, Request request, OkHttpClient client) {
        super(onOpenCallback, onMessageCallback, onClosingCallback, onClosedCallback, onFailureCallback, request, client);

    }

    @Override
    public void onOpen(WebSocket ws, Response response) {
        webSocket = ws;
        //连接成功后，设置定时器，每隔25s，自动向服务器发送心跳，保持与服务器连接
        isConnect = true;
        log.info(Instant.now().toString() + " Connected to the server success!");
        Runnable runnable = new Runnable() {
            public void run() {
                // task to run goes here
                sendMessage("ping");
            }
        };
        service = Executors.newSingleThreadScheduledExecutor();
        // 第二个参数为首次执行的延时时间，第三个参数为定时执行的间隔时间
        service.scheduleAtFixedRate(runnable, 25, 25, TimeUnit.SECONDS);
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        log.info("Connection is about to disconnect！");
        webSocket.close(1000, "Long time no message was sent or received！");
        webSocket = null;
    }

    @Override
    public void onClosed(final WebSocket webSocket, final int code, final String reason) {
        log.info("Connection dropped！");
    }

    @Override
    public void onFailure(final WebSocket webSocket, final Throwable t, final Response response) {
        if (response != null) {
            log.error(response.code() + ":" + response.body().toString());
        }
        log.error("Connection failed,Please reconnect!", t);
        if (Objects.nonNull(service)) {
            service.shutdown();
        }
    }

    @Override
    public void onMessage(final WebSocket webSocket, final String bytes) {
        //判断是否是深度接口
        if (bytes.contains("\"channel\":\"books\",") || bytes.contains("\"channel\":\"books-l2-tbt\",")) {

            if (bytes.contains("snapshot")) {//记录下第一次的全量数据
                JSONObject rst = JSON.parseObject(bytes);
                JSONObject arg = rst.getJSONObject("arg");
                JSONObject data = rst.getJSONArray("data").getJSONObject(0);
                Optional<SpotOrderBook> oldBook = parse(data.toString());
                bookMap.put(arg.get("instId").toString(), oldBook);
            } else if (bytes.contains("\"action\":\"update\",")) {//是后续的增量，则需要进行深度合并
                //log.info("action:update");
                JSONObject rst = JSON.parseObject(bytes);
                JSONObject arg = rst.getJSONObject("arg");
                JSONObject data = rst.getJSONArray("data").getJSONObject(0);
                String instrumentId = arg.get("instId").toString();
                Optional<SpotOrderBook> oldBook = bookMap.get(instrumentId);
                Optional<SpotOrderBook> newBook = parse(data.toString());
                SpotOrderBookDiff bookdiff = oldBook.get().diff(newBook.get());
                //log.debug("名称：" + instrumentId + ",深度合并成功！checknum值为：" + bookdiff.getChecksum() + ",合并后的数据为：" + bookdiff.toString());
                //String str = getStrForCheckSum(bookdiff.getAsks(), bookdiff.getBids());
                //log.debug("名称：" + instrumentId + ",拆分要校验的字符串：" + str);
                //计算checksum值
                int checksum = crc32.hashString(getStrForCheckSum(bookdiff.getAsks(), bookdiff.getBids()), StandardCharsets.UTF_8).asInt();
                //log.debug("名称：" + instrumentId + ",校验的checksum:" + checksum);

                if (checksum == bookdiff.getChecksum()) {
                    //log.debug("名称：" + instrumentId + ",深度校验通过。");
                    oldBook = parse(bookdiff.toString());
                    bookMap.put(instrumentId, oldBook);
                } else {
                    log.error("名称：" + instrumentId + ",深度校验不通过，将退订并重新订阅！");
                    //获取订阅的频道和币对
                    String unSubStr = "{\"op\": \"unsubscribe\", \"args\":[" + arg + "]}";
                    log.info(DateFormatUtils.format(new Date(), DateUtils.TIME_STYLE_S4) + " Send: " + unSubStr);
                    webSocket.send(unSubStr);
                    String subStr = "{\"op\": \"subscribe\", \"args\":[" + arg + "]}";
                    log.info(DateFormatUtils.format(new Date(), DateUtils.TIME_STYLE_S4) + " Send: " + subStr);
                    webSocket.send(subStr);
                    log.info("名称：" + instrumentId + ",正在重新订阅！");
                }
            }

        } else if (bytes.contains("candle")) {
            //k线频道
            log.debug(DateFormatUtils.format(new Date(), DateUtils.TIME_STYLE_S4) + " Receive: " + bytes);
        } else if (bytes.contains("pong")) {
            log.debug(DateFormatUtils.format(new Date(), DateUtils.TIME_STYLE_S4) + " Receive: " + bytes);
        } else {
            //不是深度\k线接口
            JSONObject rst = JSON.parseObject(bytes);
            JSONArray dataArr = rst.getJSONArray("data");
            JSONObject data = rst.getJSONArray("data").getJSONObject(0);
            Long pushTimestamp = null;
            Long localTimestamp = DateTime.now().getMillis();
            Long timing = null;
            if (dataArr.toString().contains("\"ts\"")) {
                pushTimestamp = Long.parseLong(data.get("ts").toString());
                timing = localTimestamp - pushTimestamp;
                log.debug(DateFormatUtils.format(new Date(), DateUtils.TIME_STYLE_S4) + "(" + timing + "ms)" + " Receive: " + bytes);
            } else {
                log.debug(DateFormatUtils.format(new Date(), DateUtils.TIME_STYLE_S4) + " Receive: " + bytes);
            }
        }
        if (bytes.contains("login")) {
            if (bytes.endsWith("true}")) {
                flag = true;
            }
        }
    }


    private static void isLogin(String s) {
        if (null != s && s.contains("login")) {
            if (s.endsWith("true}")) {
                flag = true;
            }
        }
    }

    //获得sign
    private static String sha256_HMAC(String message, String secret) {
        String hash = "";
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(CharsetEnum.UTF_8.charset()), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] bytes = sha256_HMAC.doFinal(message.getBytes(CharsetEnum.UTF_8.charset()));
            hash = Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            log.info("Error HmacSHA256 ===========" + e.getMessage());
        }
        return hash;
    }

    private static String listToJson(List<Map<String, String>> list) {
//        JSONArray jsonArray = new JSONArray();
//        for (Map map : list) {
//            jsonArray.add(JSONObject.from(map));
//        }
//        return jsonArray.toJSONString();
        return JSON.toJSONString(list);
    }

    //登录
    public void login(String apiKey, String passPhrase, String secretKey) {
        String timestamp = (Double.parseDouble(DateUtils.getEpochTime()) + 28800) + "";
        String message = timestamp + "GET" + "/users/self/verify";
        sign = sha256_HMAC(message, secretKey);
        String str = "{\"op\"" + ":" + "\"login\"" + "," + "\"args\"" + ":" + "[" + "\"" + apiKey + "\"" + "," + "\"" + passPhrase + "\"" + "," + "\"" + timestamp + "\"" + "," + "\"" + sign + "\"" + "]}";
        sendMessage(str);
    }


    //订阅，参数为频道组成的集合
    public void subscribe(List<Map<String, String>> list) {
        String s = listToJson(list);
        String str = "{\"op\": \"subscribe\", \"args\":" + s + "}";
        if (null != webSocket) {
            sendMessage(str);
        } else {
            log.error("webSocket is null");
        }
    }

    //取消订阅，参数为频道组成的集合
    public void unsubscribe(List<Map<String, String>> list) {
        String s = listToJson(list);
        String str = "{\"op\": \"unsubscribe\", \"args\":" + s + "}";
        if (null != webSocket)
            sendMessage(str);
    }

    private void sendMessage(String str) {
        if (null != webSocket) {
            try {
                Thread.sleep(1300);
            } catch (Exception e) {
                log.error("",e);
            }
            //log.debug(DateFormatUtils.format(new Date(), DateUtils.TIME_STYLE_S4) + "Send a message to the server:" + str);
            webSocket.send(str);
        } else {
            log.error("Please establish the connection before you operate it！");
        }
    }

    //断开连接
    public void closeConnection() {
        if (null != webSocket) {
            webSocket.close(1000, "User actively closes the connection");
        } else {
            log.error("Please establish the connection before you operate it！");
        }
    }

    private static <T extends OrderBookItem> String getStrForCheckSum(List<T> asks, List<T> bids) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < 25; i++) {
            if (i < bids.size()) {
                s.append(bids.get(i).getPrice());
                s.append(":");
                s.append(bids.get(i).getSize());
                s.append(":");
            }
            if (i < asks.size()) {
                s.append(asks.get(i).getPrice());
                s.append(":");
                s.append(asks.get(i).getSize());
                s.append(":");
            }
        }
        final String str;
        if (s.length() > 0) {
            str = s.substring(0, s.length() - 1);
        } else {
            str = "";
        }
        return str;
    }

    public static Optional<SpotOrderBook> parse(String json) {
        try {
            OrderBookData data = JSON.parseObject(json, OrderBookData.class);
            List<SpotOrderBookItem> asks = data.getAsks().stream().map(x -> new SpotOrderBookItem(x.get(0), x.get(1), x.get(2), x.get(3))).collect(Collectors.toList());
            List<SpotOrderBookItem> bids = data.getBids().stream().map(x -> new SpotOrderBookItem(x.get(0), x.get(1), x.get(2), x.get(3))).collect(Collectors.toList());
            return Optional.of(new SpotOrderBook(asks, bids, data.getTs(), data.getChecksum(), data.prevSeqId, data.seqId));
        } catch (Exception e) {
            log.error("",e);
            return Optional.empty();
        }
    }

    @Data
    public static class OrderBookData {
        private List<List<String>> asks;
        private List<List<String>> bids;
        private String ts;
        private long checksum;
        private long prevSeqId;
        private long seqId;
    }
}
