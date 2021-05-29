package com.liujun.trade_ff.core.binance.api.enums;

public enum SECURITY_TYPE {
    NONE,//	Endpoint can be accessed freely.
    TRADE,//	Endpoint requires sending a valid API-Key and signature.
    USER_DATA,//	Endpoint requires sending a valid API-Key and signature.
    USER_STREAM,//	Endpoint requires sending a valid API-Key.
    MARKET_DATA,//	Endpoint requires sending a valid API-Key.
    MARGIN
}
