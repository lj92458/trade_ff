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

1. 在 linux系统 安装java8和sqlite3
2. 根据yml文件中logging.file.path的值，创建日志目录。这里也会用来存放数据库。
3. 激活maven的prd配置，刷新maven，并打包；将安装包传输到服务器。
4. 将applicaion-prd和conf.mxl传到服务器端jar包所在目录。然后修改这两个配置文件，包括conf.xml末尾的firstBalance。prd文件里面要写weth，而不能是eth。
   配置yml时，各平台的网络名称必须相同， 而且是各平台的网络名称中共同的子字符串(okex的Avalanche和币安的avaxc,共同点是ava; 
   okex的polygon和币安的matic共同点是:币安的network字段或name字段包含polygon)。
5. 执行数据库建表/建库语句，插入初始化数据。
6. 需要授权某合约能花费自己的token。不能从uniswap界面上操作，而是要调用trade.js里面的getTokenTransferApproval函数，对SwapRouter合约授权。或者在etherscan上调用。
7. cex的充值地址，要加入js项目的白名单；dex的充值地址，也要在okx白名单(每种网络都有自己的白名单)、在币安认证过的地址。
8. 启动命令： nohup java -jar trade_ff-0.0.1-SNAPSHOT.jar --appName=arbitrum --okx.passphrase=xxx  >>out.txt &
   
#### 使用说明

1. 为了方便随时编辑application-prd.yml，将配置文件放到了jar包所在目录，这里的优先级高于jar包内。[参考spring官网](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#features.external-config.files)
2. 在maven窗口切换了profile复选框，记得要刷新maven并重新打包，才能生效。
3. 开发调试，既可以调试本地打包好的jar文件，也能直接debug源代码的main方法。如果是main方法，需要改变idea工作目录：配置debug选项 -> (超链接)修改选项 -> 工作目录 -> 把工作目录设定为target目录，
   这样Engine类才能从当前目录读取到conf.mxl 。调试前，记得激活maven的dev配置，并刷新maven.
4. op链：eth/usdc(7天交易75M)  https://api-optimistic.etherscan.io/api  key=5VR1M7IQYFPZBU189F8ABH4W4NIHJ9GYJQ
   arb链：eth/usdc(7天交易900M)、arb/usdc   https://api.arbiscan.io/api  key=32YQ9W1FDCU1XGCUNQQF9Z5GG6R5B2BYNI    或者nova(安全性下降)   https://api-nova.arbiscan.io/api   key=FMS97MUY87E7QXNP7WA7TXMSPP7T11N9HD     
   Polygon链：eth/usdc(7天交易120M)、matic/usdc!!      https://api.polygonscan.com/api     key=IIE5BC2IDCKEQ88UBZ61HIDCS8TE7UIYQZ

bsc链上做 btc/usdt !!!   btc/bnb   7天交易量11M，太小了，希望有百兆

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
