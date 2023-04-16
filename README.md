# trade
两个现货平台之间套利
观测：将rpcServer端slippage提高到0.03，java端atLeastRate
降到0.004
#### 介绍
数字货币自动交易/对冲套利/赚差价/量化交易。支持中心化平台和去中心化平台。
集成了springMVC，sqlite，webSocket。将日志显示到页面。
#### 软件架构
软件架构说明：springMVC, 用hprose做RPC，实现跨语言调用(调用别人用js/python开发的功能模块)。


#### 安装教程

1.  在 linux系统 安装java8和sqlite3
2.  根据yml文件中log.path的值，创建日志目录。这里也会用来存放数据库。
3.  修改配置文件： applicaion-prd，把conf.xml放到日志目录并修改firstBalance。
    删除logback-test-spring.xml(不用删除，因为有logging.config参数决定用哪个)
4. 激活maven的prd配置，并打包；将安装包传输到服务器
5. 执行数据库建表/建库语句，插入初始化数据。
6.需要授权某合约能花费自己的token 

#### 使用说明

1.  xxxx
2.  xxxx
3.  xxxx

#### 参与贡献

1.  Fork 本仓库
2.  新建 Feat_xxx 分支
3.  提交代码
4.  新建 Pull Request


#### 码云特技

1.  使用 Readme\_XXX.md 来支持不同的语言，例如 Readme\_en.md, Readme\_zh.md
2.  码云官方博客 [blog.gitee.com](https://blog.gitee.com)
3.  你可以 [https://gitee.com/explore](https://gitee.com/explore) 这个地址来了解码云上的优秀开源项目
4.  [GVP](https://gitee.com/gvp) 全称是码云最有价值开源项目，是码云综合评定出的优秀开源项目
5.  码云官方提供的使用手册 [https://gitee.com/help](https://gitee.com/help)
6.  码云封面人物是一档用来展示码云会员风采的栏目 [https://gitee.com/gitee-stars/](https://gitee.com/gitee-stars/)
