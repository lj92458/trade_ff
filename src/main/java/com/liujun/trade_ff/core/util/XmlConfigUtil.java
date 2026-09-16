package com.liujun.trade_ff.core.util;

import org.dom4j.Document;
import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

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
        InputStreamReader reader = new InputStreamReader(Files.newInputStream(Paths.get(filePath)), charset);
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

    /**
     * 读取xml元素的属性
     */
    public static String readXmlAttribute(String filePath, String elementPath, String attrName) throws Exception {
        InputStreamReader reader = new InputStreamReader(Files.newInputStream(Paths.get(filePath)), charset);
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
     * 保存xml某元素的值，或多个属性。
     *
     * @param xmlDoc      内存中的Document对象。它的优先级比filePath高，可以不传
     * @param filePath    文件路径。必须提供
     * @param elementPath 元素路径
     * @param attr        如果不是null，就是保存元素属性；是null就是保存元素值
     * @param value       值
     * @throws Exception
     */
    public static void saveXmlAttribute(Document xmlDoc, String filePath, String elementPath, String attr, String value)
            throws Exception {
        synchronized (XmlConfigUtil.class) {//对xmlDoc的写操作，可能引发线程安全问题，所以要加锁
            if (xmlDoc == null) {
                InputStreamReader reader = new InputStreamReader(Files.newInputStream(Paths.get(filePath)), charset);
                SAXReader sax = new SAXReader();
                xmlDoc = sax.read(reader);
                reader.close();
            }
            if (attr == null) {
                xmlDoc.selectSingleNode(elementPath).setText(value);
            } else {
                xmlDoc.selectSingleNode(elementPath + "/@" + attr).setText(value);
            }

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
