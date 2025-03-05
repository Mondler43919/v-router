package vRouter;

import kademlia.KademliaCommonConfig;
import kademlia.UniformRandomGenerator;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public class QueryGenerator implements Control {
    // 控制查询生成是否执行的标志
    public static boolean executeFlag = false;

    // 存储可用数据的队列
    public static Queue<BigInteger> availableData = new LinkedList<>();

    // 存储查询过的数据及其查询次数
    public static HashMap<BigInteger,Integer> queriedData = new HashMap<>();

    // 用于记录查询路径
    public static ArrayList<BigInteger> indexPath = new ArrayList<>();
    public static ArrayList<BigInteger> indexToPath = new ArrayList<>();
    public static ArrayList<BigInteger> queryPath = new ArrayList<>();

    // 调试目标数据，供调试时使用
    public static final BigInteger DEBUGTARGET = new BigInteger("1114055198376486755617044701041245316474664586947");

    // 协议的参数和ID
    private final static String PAR_PROT = "protocol";
    private final int pid;

    // 随机数生成器，用于生成随机查询数据
    UniformRandomGenerator urg;

    // 构造函数，初始化协议ID和随机数生成器
    public QueryGenerator(String prefix) {
        pid = Configuration.getPid(prefix + "." + PAR_PROT);  // 从配置文件中获取协议ID
        urg = new UniformRandomGenerator(KademliaCommonConfig.BITS, CommonState.r);  // 初始化随机数生成器
    }

    /**
     * 每次调用该控制器都会生成并发送10个随机的数据查询请求
     *
     * @return boolean 返回false表示不继续执行
     */
    public boolean execute() {
        // 如果未启用查询生成器，则不执行查询操作
        if (!executeFlag) {
            return false;
        }

        Node start;
        // 随机选择一个网络中的节点作为查询的起始节点
        do {
            start = Network.get(CommonState.r.nextInt(Network.size()));
        } while ((start == null) || (!start.isUp()));  // 确保选择的节点是存在且处于活动状态的

        // 从可用数据队列中取出一个查询项
        BigInteger query = availableData.poll();
        if (query == null) return false;  // 如果没有可用查询数据，则返回

        // 将查询数据加入已查询的数据记录
        queriedData.put(query, 0);
        VRouterObserver.dataQueryTraffic.put(query, 0);

        // 获取查询起始节点的协议实例
        VRouterProtocol p = (VRouterProtocol) start.getProtocol(pid);

        // 将查询请求添加到该节点的查找消息队列中
        p.lookupMessages.add(new VLookupMessage(query, p.nodeId));

        return false;  // 返回false，表示不继续执行
    }

} // 类结束
// ___________________________________________________________
