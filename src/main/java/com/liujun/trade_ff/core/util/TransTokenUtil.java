package com.liujun.trade_ff.core.util;

import com.liujun.trade_ff.core.Engine;
import com.liujun.trade_ff.core.Trade;
import com.liujun.trade_ff.core.util.graph.Graph;
import com.liujun.trade_ff.core.util.graph.Node;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class TransTokenUtil {
    @ToString
    public static class TransRoute {
        public Trade fromTrade;
        public Trade toTrade;
        public String netWorkShort;//简短的网络名称，跟yml中配置的一致。例如avax又叫Avalanche,它们的共同部分就是ava

        public TransRoute(Trade fromTrade, Trade toTrade, int tokenIndex) throws Exception {
            this.fromTrade = fromTrade;
            this.toTrade = toTrade;
            //寻找它们共有的netWork
            outFor:
            for (String netWork : fromTrade.tokenNetWork[tokenIndex]) {
                for (String netWork2 : toTrade.tokenNetWork[tokenIndex]) {
                    if (netWork.equals(netWork2)) {
                        netWorkShort = netWork;
                        break outFor;
                    }
                }
            }
            if (netWorkShort == null) {
                throw new Exception("两个trade并没有共同的网络:" + fromTrade.getPlatName() + "_" + toTrade.getPlatName());
            }
        }
    }

    /**
     * @param fromTrade 发送
     * @param toTrade   接收
     * @param amount    金额
     * @return 最终收到的金额
     * @throws Exception
     */
    public static double trans(Engine engine, Trade fromTrade, Trade toTrade, int tokenIndex, double amount) throws Exception {
        List<TransRoute> routes = createRoutes(engine, fromTrade, toTrade, tokenIndex);
        log.info("route数量：" + routes.size() + ", 内容：" + routes);
        if (routes.size() == 0) {
            throw new Exception("routes数量不能是0");
        }
        //经过多次路由跳转，会产生损耗。amount可能会越来越小呢。所以把每次的收款金额，作为下一次的付款金额。否则可能会余额不足
        double receiveAmount = amount;
        for (TransRoute route : routes) {
            receiveAmount = transOneRoute(route, tokenIndex, receiveAmount);
        }
        return receiveAmount;
    }

    /**
     * 路由算法主要有：1.迪杰斯特拉算法（Dijkstra算法，贪心算法，处理单源最短路径）
     * 2.弗洛伊德算法（Floyd算法，动态规划，处理多源最短路径，更强大也更复杂）
     * 我们这里用Dijkstra(它也可以处理有环图)
     *
     * @param fromTrade  发送
     * @param toTrade    接收
     * @param tokenIndex 0代表goods，1代表money
     * @return 路由列表
     */
    public static List<TransRoute> createRoutes(Engine engine, Trade fromTrade, Trade toTrade, int tokenIndex) throws Exception {
        List<TransRoute> routes = new ArrayList<>();
        //
        Graph graph = new Graph();
        List<Node> nodes = new ArrayList<>();
        for (Trade t : engine.actualPlats()) {
            Node node = new Node(t.getPlatName());
            nodes.add(node);
            graph.addNode(node);
        }
        //给各node添加邻居节点
        for (Node node : nodes) {
            Set<Node> nodeSet = findNeighbors(nodes, node.getName(), engine.actualPlats(), tokenIndex);
            nodeSet.forEach(o -> node.addDestination(o, 1));
        }
        //为每个节点生成从toTrade到该节点的最短路径。
        Graph.calculateShortestPathFromSource(graph, nodes.stream().filter(o -> o.getName().equals(fromTrade.getPlatName())).findFirst().get());
        //为toTrade找到最短路径
        List<String> nameList = nodes.stream().filter(o -> o.getName().equals(toTrade.getPlatName())).findFirst().get()
                .getShortestPath().stream().map(Node::getName).collect(Collectors.toList());
        if (nameList.size() > 0) {
            //开始创建路由. 两个平台之间，就是一个路由
            nameList.add(toTrade.getPlatName());
            log.info("找到路由：" + nameList);
            for (int i = 0; i < nameList.size() - 1; i++) {
                Trade t1 = findPlatByName(nameList.get(i), engine);
                Trade t2 = findPlatByName(nameList.get(i + 1), engine);
                routes.add(new TransRoute(t1, t2, tokenIndex));
            }
        } else {
            log.error("createRoutes找不到路由");
        }

        return routes;
    }

    public static Trade findPlatByName(String name, Engine engine) {
        for (Trade t : engine.actualPlats()) {
            if (t.getPlatName().equals(name)) {
                return t;
            }
        }
        return null;
    }

    /**
     * 查找某平台的邻居
     *
     * @param platName   平台名称
     * @param trades     平台列表
     * @param tokenIndex 0代表goods，1代表money
     * @return 平台列表编号
     */
    public static Set<Node> findNeighbors(List<Node> nodes, String platName, List<Trade> trades, int tokenIndex) {
        Set<Node> nodeSet = new HashSet<>();
        //找到platName代表的那个trade,然后让它跟别的trade匹配
        Trade fromTrade = trades.stream().filter(o -> o.getPlatName().equals(platName)).findFirst().get();
        for (String netWork : fromTrade.tokenNetWork[tokenIndex]) {
            for (Trade trade : trades) {
                if (trade != fromTrade && Arrays.asList(trade.tokenNetWork[tokenIndex]).contains(netWork)) {
                    nodeSet.add(nodes.stream().filter(o -> o.getName().equals(trade.getPlatName())).findFirst().get());
                }
            }
        }
        return nodeSet;
    }

    /**
     * @param route
     * @param tokenIndex
     * @param amount
     * @return amount被扣除手续费后，还剩多少
     * @throws Exception
     */
    public static double transOneRoute(TransRoute route, int tokenIndex, double amount) throws Exception {
        log.info("开始对route发起转账: " + route);

        //是否需要wrap. 如果route.fromTrade作为dex负责发送，route.toTrade却要求接收eth
        boolean withdrawNeedWrap = route.fromTrade.fixFee > 0
                && route.fromTrade.token[tokenIndex].equalsIgnoreCase("w" + route.toTrade.token[tokenIndex])
                && route.toTrade.token[tokenIndex].equalsIgnoreCase(route.fromTrade.getNaitveToken());
        //route.fromTrade发送什么币. 默认和route.fromTrade一致,满足withdrawNeedWrap条件才会用NaitveToken
        String sendToken = withdrawNeedWrap ? route.fromTrade.getNaitveToken() : route.fromTrade.token[tokenIndex];

        String txId = route.fromTrade.withdraw(sendToken, amount, route.toTrade.getTokenAddress()[tokenIndex], route.netWorkShort, withdrawNeedWrap);

        //from发送完了，to开始接收
        if (StringUtils.isNotEmpty(txId)) {
            //收到币后，是否需要wrap. to作为dex接收到eth，to却只想要weth 就应该转换
            boolean depositNeedWrap = route.toTrade.fixFee > 0
                    && route.toTrade.token[tokenIndex].equalsIgnoreCase("w" + sendToken)
                    && sendToken.equalsIgnoreCase(route.toTrade.getNaitveToken());

            double receiveAmount = route.toTrade.depositToken(sendToken, txId, amount, depositNeedWrap);//from发送什么币，to就接收什么
            if (receiveAmount > 0) {
                log.info(route.toTrade.getPlatName() + "收款成功，receiveAmount=" + receiveAmount);
                return receiveAmount;
            } else {
                throw new Exception(route.toTrade.getPlatName() + "收款异常 receiveAmount=" + receiveAmount);
            }
        } else {
            throw new Exception(route.fromTrade.getPlatName() + ".withdraw发生异常:返回的txId为空");
        }


    }
}
