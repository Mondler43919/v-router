package vRouter;

import kademlia.*;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

/**
 * 此控制器生成随机的查询流量，节点向随机目标节点发送查询请求。
 *
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */
public class DataGenerator implements Control {

	// ______________________________________________________________________________________________
	/**
	 * MSPastry 协议的协议 ID
	 */
	private final static String PAR_PROT = "protocol";
	private final static String TURNS = "turns";  // 数据生成的周期
	private final static String CYCLES = "cycles"; // 模拟总周期

	/**
	 * MSPastry 协议 ID
	 */
	private final int pid;
	private int dataGenerateSimCycle = Integer.MAX_VALUE; // 数据生成周期
	private int totalSimCycle = Integer.MAX_VALUE; // 模拟总周期
	UniformRandomGenerator urg;

	private int turns = 0;

	// ______________________________________________________________________________________________
	/**
	 * 构造函数，初始化协议 ID 和配置参数
	 * @param prefix 配置前缀
	 */
	public DataGenerator(String prefix) {
		pid = Configuration.getPid(prefix + "." + PAR_PROT); // 获取协议 ID
		dataGenerateSimCycle = Configuration.getInt(prefix + "." + TURNS); // 获取数据生成周期
		totalSimCycle = Configuration.getInt(prefix + "." + CYCLES); // 获取模拟总周期
		urg = new UniformRandomGenerator(KademliaCommonConfig.BITS, CommonState.r); // 初始化随机数生成器
	}

	// ______________________________________________________________________________________________
	/**
	 * 每次调用此控制器时，生成并发送 100 条随机数据存储消息
	 *
	 * @return boolean
	 */
	public boolean execute() {
		turns++;  // 增加当前模拟轮次

		// 如果轮次已经达到数据生成周期 + 20，启动查询，不再生成数据
		if(turns >= dataGenerateSimCycle + 20){
			// 如果已完成最后一次查询并且没有更多新的查询，则停止查询生成
			if(turns + 20 >= totalSimCycle){
				QueryGenerator.executeFlag = false;  // 停止查询生成
				return false;
			}
			QueryGenerator.executeFlag = true;  // 启动查询生成
			return false;
		}

		// 如果轮次还没有到达数据生成周期，则停止新数据的存储
		if(turns >= dataGenerateSimCycle) return false;

		// 每次调用时生成 100 条随机数据
		for(int i=0; i<100; i++){
			Node start;
			do {
				// 随机选择一个节点
				start = Network.get(CommonState.r.nextInt(Network.size()));
			} while ((start == null) || (!start.isUp()));  // 确保选择的节点是活动的

			VRouterProtocol p = (VRouterProtocol) start.getProtocol(pid);  // 获取节点的协议

			BigInteger dataID = urg.generate();  // 生成一个随机的数据 ID
			QueryGenerator.availableData.add(dataID);  // 将生成的数据 ID 添加到查询生成器的队列中
			VRouterObserver.dataIndexTraffic.put(dataID, 0);  // 记录数据 ID 的流量统计
			p.storeData(dataID, pid);  // 存储数据
		}
		return false;  // 执行完成
	}

	// ______________________________________________________________________________________________

} // 类结束
