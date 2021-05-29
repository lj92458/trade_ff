/**
 * Copyright 2020 bejson.com
 */
package com.liujun.trade_ff.core.binance.api.bean.spot.result;
import java.util.List;

/**
 * Auto-generated: 2020-06-16 20:12:18
 *
 * @author bejson.com (i@bejson.com)
 * @website http://www.bejson.com/java2pojo/
 */
public class Depth {

    private long lastUpdateId;
    private List<String[]> bids;
    private List<String[]> asks;
    /*
    {
  "lastUpdateId": 1027024,
  "bids": [
    [
      "4.00000000",     // 价位
      "431.00000000"    // 挂单量
    ]
  ],
  "asks": [
    [
      "4.00000200",
      "12.00000000"
    ]
  ]
}
     */
    public void setLastUpdateId(long lastUpdateId) {
        this.lastUpdateId = lastUpdateId;
    }
    public long getLastUpdateId() {
        return lastUpdateId;
    }

    public List<String[]> getBids() {
        return bids;
    }

    public void setBids(List<String[]> bids) {
        this.bids = bids;
    }

    public List<String[]> getAsks() {
        return asks;
    }

    public void setAsks(List<String[]> asks) {
        this.asks = asks;
    }
}