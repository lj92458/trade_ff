package com.liujun.trade_ff.core.binance.api.utils;

import org.apache.commons.beanutils.PropertyUtils;

import java.util.Map;

public class MapUtil {

    /**
     * 把空值变成字符串""
     * @param obj
     * @return
     * @throws Exception
     */
    public static Map<String, Object> toMap(Object obj) throws Exception {
        Map<String, Object> map = PropertyUtils.describe(obj);
        for (String key : map.keySet()) {
            map.putIfAbsent(key, "");
        }
        return map;
    }
}
