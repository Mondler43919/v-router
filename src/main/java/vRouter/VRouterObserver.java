package vRouter;

import com.google.gson.Gson;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.util.IncrementalStats;

import java.math.BigInteger;
import java.util.HashMap;

/**
 * 该类实现了一个简单的观察器，用于观察网络中查找节点时的时间和跳数平均值
 *
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */
public class VRouterObserver implements Control {

	/**
	 * 记录每次成功查找消息的跳数（转发跳数）
	 */
	public static IncrementalStats successLookupForwardHop = new IncrementalStats();

	/**
	 * 记录每次成功查找消息的跳数（反向跳数）
	 */
	public static IncrementalStats successLookupBackwardHop = new IncrementalStats();

	/**
	 * 记录每次查找消息的跳数（目前未使用）
	 */
//	public static IncrementalStats droppedLookupMessage = new IncrementalStats();

//	public static IncrementalStats totalQuery = new IncrementalStats();

	/**
	 * 记录每次成功查找消息的总跳数
	 */
	public static IncrementalStats totalSuccessHops = new IncrementalStats();

	/**
	 * 记录失败查找消息的跳数
	 */
	public static IncrementalStats failedBackwardLookupHop = new IncrementalStats();

	/**
	 * 记录索引消息的跳数
	 */
	public static IncrementalStats indexHop = new IncrementalStats();

	/**
	 * 记录Bloom Filter消息的跳数
	 */
	public static IncrementalStats bloomFilterCount = new IncrementalStats();

	// 存储每个数据索引的流量统计
	public static HashMap<BigInteger,Integer> dataIndexTraffic = new HashMap<>();

	// 存储每个数据查询的流量统计
	public static HashMap<BigInteger,Integer> dataQueryTraffic = new HashMap<>();

	/** 协议参数，用于配置和观察 */
	private static final String PAR_PROT = "protocol";

	/** 协议ID */
	private int pid;

	/** 输出时使用的前缀 */
	private String prefix;

	// 新增统计节点活跃度评分的统计数据
	public static IncrementalStats activityScoreStats = new IncrementalStats();

	// 记录中心节点切换的次数
	private static int centralNodeSwitchCount = 0;

	// 构造方法，初始化协议前缀并通过配置文件获取协议ID
	public VRouterObserver(String prefix) {
		this.prefix = prefix;
		pid = Configuration.getPid(prefix + "." + PAR_PROT);
	}

	/**
	 * 输出当前的统计快照
	 *
	 * @return 始终返回false，表示该控制器不会继续执行
	 */
	public boolean execute() {
		// 获取真实的网络节点数量
		int sz = Network.size();
		// 统计处于激活状态的节点数量
		for (int i = 0; i < Network.size(); i++)
			if (!Network.get(i).isUp())
				sz--;

		// 存储效率评估信息
		String storeS = String.format("[time=%d]:[N=%d 当前节点UP] [D=%f 最大索引跳数] [%f 平均索引跳数] [%d 总BF数量] [%f 每个节点平均BF数量]",
				CommonState.getTime(),
				sz,
				indexHop.getMax(),
				indexHop.getAverage(),
				bloomFilterCount.getN(),
				((float) bloomFilterCount.getN() / (float) Network.size())
		);

		// 存储Bloom Filter数量的统计信息
		String storeC = String.format("%f",
				((float) bloomFilterCount.getN() / (float) Network.size())
		);

		// 存储索引跳数的统计信息
		String indexC = String.format("%f",
				((float) bloomFilterCount.getN() / (float) Network.size())
		);

		// 查询效率评估信息
		String queryS = String.format("%f,%f",
				totalSuccessHops.getMax(),
				totalSuccessHops.getAverage()
		);

		// 输出查询效率统计信息
		System.err.println(queryS);

		// 输出节点活跃度评分统计信息
		System.err.println(String.format("[Time=%d] 活跃度评分的最大值: %f, 平均值: %f",
				CommonState.getTime(),
				activityScoreStats.getMax(),
				activityScoreStats.getAverage()
		));

		// 输出中心节点切换次数
		System.err.println("中心节点切换次数: " + centralNodeSwitchCount);

		return false; // 返回false表示该控制器不再继续执行
	}

	/**
	 * 记录节点活跃度评分
	 *
	 * @param score 节点评分
	 */
	public static void recordActivityScore(double score) {
		activityScoreStats.add(score);  // 记录评分数据
	}

	/**
	 * 记录中心节点切换次数
	 */
	public static void recordCentralNodeSwitch() {
		centralNodeSwitchCount++;  // 增加中心节点切换次数
	}
}
