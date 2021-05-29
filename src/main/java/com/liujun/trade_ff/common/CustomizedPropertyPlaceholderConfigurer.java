package com.liujun.trade_ff.common;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 自定义 配置文件处理类
 * Created by WuShaotong on 2016/6/21.
 */
public class CustomizedPropertyPlaceholderConfigurer extends PropertyPlaceholderConfigurer {

    private static Map<String, Object> ctxPropertiesMap;

    @Override
    protected void processProperties(ConfigurableListableBeanFactory beanFactoryToProcess, Properties props) throws BeansException {
        super.processProperties(beanFactoryToProcess, props);
        ctxPropertiesMap = new HashMap<String, Object>();
        for(Object key : props.keySet()){
            String keyStr = key.toString();
            String value = props.getProperty(keyStr);
            ctxPropertiesMap.put(keyStr, value);
        }
    }

    /**
     * 获取配置文件
     * @param key 键
     * @return 值
     */
    public static Object getContextProperty(String key){
        return ctxPropertiesMap.get(key);
    }
}
