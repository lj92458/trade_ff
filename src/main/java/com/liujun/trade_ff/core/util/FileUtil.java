package com.liujun.trade_ff.core.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileUtil {
	/**
	 * 从文本文件读取倒数lineNum行
	 * 
	 * @param fileName
	 *            文件路径、名称
	 * @param charset
	 *            字符编码
	 * @param lineNum
	 *            读取的行数
	 * @return 字符串List
	 * @throws IOException
	 */
	public static List<String> readLastLine(String fileName, String charset, int lineNum) throws IOException {
		List<String> strList = new ArrayList<String>();
		File file = new File(fileName);
		if (!file.exists() || file.isDirectory() || !file.canRead()) {
			return strList;
		}
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(file, "r");
			long len = raf.length();
			if (len == 0L) {
				return strList;
			} else {
				long endPos = len;// 读取到此处结束
				for (long pos = endPos - 1; pos >= 0 && strList.size() < lineNum; endPos = pos, pos--) {
					for (; pos >= 0; pos--) {
						raf.seek(pos);
						if (raf.readByte() == '\n') {
							break;
						}
					}// end inner for
					raf.seek(pos + 1);// 从换行字符的下一个开始读
					if (pos < len - 1) {
						byte[] bytes = new byte[(int) (endPos - (pos + 1))];
						raf.read(bytes);
						if (charset == null) {
							strList.add(new String(bytes));
						} else {
							strList.add(new String(bytes, charset));
						}
					}// end if
				}// end for
					// 把顺序调整过来
				Collections.reverse(strList);
				return strList;
			}// end else
		} catch (FileNotFoundException e) {
		} finally {
			if (raf != null) {
				try {
					raf.close();
				} catch (Exception e2) {
				}
			}
		}
		return null;
	}
}
