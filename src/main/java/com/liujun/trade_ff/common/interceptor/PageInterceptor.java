package com.liujun.trade_ff.common.interceptor;


import com.liujun.trade_ff.bean.Page;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

/** 利用拦截器 来进行mybaits分页
 * Created by pengsc on 2016/8/5 0005.
 */
@Intercepts({ @Signature(method = "prepare", type = StatementHandler.class, args = { Connection.class,Integer.class }) })
public class PageInterceptor implements Interceptor {
    private static final Logger logger = LoggerFactory.getLogger(PageInterceptor.class);
    private String SELECT_ID="splitpage"; //方法名中包含此名，则调用分页方法

    private String databaseType;// 数据库类型，不同的数据库有不同的分页方法

    /**
     * 拦截后要执行的方法
     */
    //插件运行的代码，它将代替原有的方法
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        logger.debug("PageInterceptor -- intercept "+invocation.getTarget());
        if (invocation.getTarget() instanceof StatementHandler) {
            StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
            MetaObject metaStatementHandler = SystemMetaObject.forObject(statementHandler);
            MappedStatement mappedStatement=(MappedStatement) metaStatementHandler.getValue("delegate.mappedStatement");
            String selectId=mappedStatement.getId();
            logger.debug("方法名： "+selectId);
            if((selectId.substring(selectId.lastIndexOf(".")+1).toLowerCase()).contains(SELECT_ID)){
                BoundSql boundSql = (BoundSql) metaStatementHandler.getValue("delegate.boundSql");
                // 分页参数作为参数对象parameterObject的一个属性
                String sql = boundSql.getSql();
                logger.info(sql);
                logger.info(boundSql.getParameterObject()+"-----------");
                Page co=(Page) (boundSql.getParameterObject());

                // 重写sql
                String countSql=concatCountSql(sql);
                String pageSql=getPageSql(co,sql);

                logger.info("重写的查询总数的sql	:"+countSql);
                logger.info("重写的查询记录的sql	:"+pageSql);

                Connection connection = (Connection) invocation.getArgs()[0];

                //通过BoundSql获取对应的参数映射
                List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
                //利用Configuration、查询记录数的Sql语句countSql、参数映射关系parameterMappings和参数对象page建立查询记录数对应的BoundSql对象。
                BoundSql countBoundSql = new BoundSql(mappedStatement.getConfiguration(), countSql, parameterMappings, co);
                //通过mappedStatement、参数对象page和BoundSql对象countBoundSql建立一个用于设定参数的ParameterHandler对象
                ParameterHandler parameterHandler = new DefaultParameterHandler(mappedStatement, co, countBoundSql);
                PreparedStatement countStmt = null;
                ResultSet rs=null;
                int totalCount = 0;
                try {
                    countStmt = connection.prepareStatement(countSql);
                    //通过parameterHandler给PreparedStatement对象设置参数
                    parameterHandler.setParameters(countStmt);
                    rs = countStmt.executeQuery();
                    if (rs.next()) {
                        totalCount = rs.getInt(1);
                    }

                } catch (SQLException e) {
                    System.out.println("Ignore this exception"+e);
                } finally {
                    try {
                        if(rs!=null){
                            rs.close();
                        }
                        if(countStmt!=null){
                            countStmt.close();
                        }
                    } catch (SQLException e) {
                        System.out.println("Ignore this exception"+ e);
                    }
                }

                metaStatementHandler.setValue("delegate.boundSql.sql", pageSql);

                //绑定count
                co.setRowTotal(totalCount);
                int pageTotal = totalCount / co.getPageSize();
                if(totalCount % co.getPageSize() > 0){
                    pageTotal++;
                }
                co.setPageTotal(pageTotal);
            }
        }

         return invocation.proceed();

    }

    // 拦截类型StatementHandler
    @Override
    public Object plugin(Object target) {
        if (target instanceof StatementHandler) {
            return Plugin.wrap(target, this);
        } else {
            return target;
        }
    }
    /**
     * 设置注册拦截器时设定的属性
     */
    @Override
    public void setProperties(Properties properties) {
        this.databaseType = properties.getProperty("databaseType");
    }

    public String concatCountSql(String sql){
        String orderBy="order by ";
        StringBuffer sb=new StringBuffer("select count(*) ");
        sql=sql.toLowerCase().replaceAll("\\s{2,}"," ");

        if(sql.lastIndexOf(orderBy)>sql.lastIndexOf(")")){
            sb.append(sql.substring(sql.indexOf(" from "), sql.lastIndexOf(orderBy)));
        }else{
            sb.append(sql.substring(sql.indexOf(" from ")));
        }
        return sb.toString();
    }



    public void setPageCount(){

    }

    /**
     * 根据page对象获取对应的分页查询Sql语句，这里只做了两种数据库类型，Mysql和Oracle 其它的数据库都 没有进行分页
    */
    private String getPageSql(Page page, String sql) throws Exception{
        final StringBuffer sqlBuffer = new StringBuffer(sql);
        if ("mysql".equalsIgnoreCase(databaseType)) {
            return getMysqlPageSql(page, sqlBuffer);
        } else if ("oracle".equalsIgnoreCase(databaseType)) {
//            return getOraclePageSql(page, sqlBuffer);
        }else if("sqlite".equalsIgnoreCase(databaseType)){
            //return getMysqlPageSql(page, sqlBuffer);
        }else if(null==databaseType||"".equals(databaseType)){
            throw new Exception("databaseType值是空的（获取不到数据类型）");
        }
        return sqlBuffer.toString();
    }
    /**
     * 获取Mysql数据库的分页查询语句
     */
    private String getMysqlPageSql(Page page, StringBuffer sqlBuffer) {
        // 计算第一条记录的位置，Mysql中记录的位置是从0开始的。
        // int offset = (page.getPage().getPageIndex() - 1) *
        // page.getPageSize();
        sqlBuffer.append(" limit ").append(page.getPagebegin()).append(",").append(page.getPageSize());
        return sqlBuffer.toString();
    }
}