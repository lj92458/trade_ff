package com.liujun.trade_ff.core.util;

import org.dom4j.Document;
import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.*;

public class XmlConfigUtil {
    static String charset = "utf-8";

    /**
     * 从xml配置文件中读取参数
     * 
     * @param elementPath
     * @return
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     */
    public static String readXmlProp(String filePath, String elementPath) throws Exception {
        InputStreamReader reader = new InputStreamReader(new FileInputStream(filePath), charset);
        SAXReader sax = new SAXReader();
        Document xmlDoc = sax.read(reader);
        reader.close();
        String path = elementPath; //.replace("_", "/");
        Node node = xmlDoc.selectSingleNode(path);
        if (node == null) {
            return null;
        } else {
            return node.getText();
        }
    }

    /** 读取xml元素的属性 */
    public static String readXmlAttribute(String filePath, String elementPath, String attrName) throws Exception {
        InputStreamReader reader = new InputStreamReader(new FileInputStream(filePath), charset);
        SAXReader sax = new SAXReader();
        Document xmlDoc = sax.read(reader);
        reader.close();
        String path = elementPath + "/@" + attrName;
        Node node = xmlDoc.selectSingleNode(path);
        if (node == null) {
            return null;
        } else {
            return node.getText();
        }
    }

    /**
     * 保存xml参数
     * 
     * @param elementPath
     * @param value
     * @throws Exception
     */
    public static void saveXmlProp(String filePath, String elementPath, String value) throws Exception {
        synchronized (XmlConfigUtil.class) {//对xmlDoc的写操作，可能引发线程安全问题，所以要加锁
            InputStreamReader reader = new InputStreamReader(new FileInputStream(filePath), charset);
            SAXReader sax = new SAXReader();
            Document xmlDoc = sax.read(reader);
            reader.close();
            String path = elementPath;
            xmlDoc.selectSingleNode(path).setText(value);
            FileOutputStream fos = new FileOutputStream(filePath, false);
            OutputFormat format = OutputFormat.createPrettyPrint();
            format.setEncoding(charset);
            XMLWriter xmlWriter = new XMLWriter(fos, format);
            xmlWriter.write(xmlDoc);
            xmlWriter.flush();
            xmlWriter.close();
            fos.close();
        }
    }

    /**
     * 保存xml元素的属性
     * 
     * @param elementPath
     * @param value
     * @throws Exception
     */
    public static void saveXmlAttribute(String filePath, String elementPath, String attrName, String value)
            throws Exception {
        synchronized (XmlConfigUtil.class) {//对xmlDoc的写操作，可能引发线程安全问题，所以要加锁
            InputStreamReader reader = new InputStreamReader(new FileInputStream(filePath), charset);
            SAXReader sax = new SAXReader();
            Document xmlDoc = sax.read(reader);
            reader.close();
            String path = elementPath + "/@" + attrName;
            xmlDoc.selectSingleNode(path).setText(value);
            FileOutputStream fos = new FileOutputStream(filePath, false);
            OutputFormat format = OutputFormat.createPrettyPrint();
            format.setEncoding(charset);
            XMLWriter xmlWriter = new XMLWriter(fos, format);
            xmlWriter.write(xmlDoc);
            xmlWriter.flush();
            xmlWriter.close();
            fos.close();
        }
    }
}
