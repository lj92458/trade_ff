package com.liujun.trade_ff.core.util;

public class StringUtil {

	public static boolean isEmpty(String str) {
		if (str == null)
			return true;
		String tempStr = str.trim();
		if (tempStr.length() == 0)
			return true;
		if (tempStr.equals("null"))
			return true;
		return false;
	}

	public static boolean notEmpty(String str) {

		return !isEmpty(str);
	}
}