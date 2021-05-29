package com.liujun.trade_ff.core.binance.api.enums;

/**
 * Http Headers Enum . <br/>
 * All REST requests must contain the following headers. <br/>
 * The api key and secret key will be randomly generated and provided by OKEX. <br/>
 * The Passphrase will be provided by you to further secure your API access. <br/>
 * OKEX stores the salted hash of your passphrase for verification, but cannot recover the passphrase if you forget it.<br/>
 * OKEX cursor pagination response headers.<br/>
 * Request page before (newer) and after (older) this pagination id,
 * and limit number of results per request. maximum 100. (default 100). <br/>
 *
 * @author Tony Tian
 * @version 1.0.0
 * @date 2018/2/5 20:45
 */
public enum HttpHeadersEnum {

     X_MBX_APIKEY("X-MBX-APIKEY"),
     SECURITY_TYPE("SECURITY_TYPE");

    private final String header;

    HttpHeadersEnum(final String header) {
        this.header = header;
    }

    public String header() {
        return this.header;
    }
}
