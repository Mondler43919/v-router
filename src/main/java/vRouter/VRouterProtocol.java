package vRouter;

/**
 * A Kademlia implementation for PeerSim extending the EDProtocol class.<br>
 * See the Kademlia bibliography for more information about the protocol.
 *
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */

import kademlia.*;
import peersim.cdsim.CDProtocol;
import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;

import java.math.BigInteger;
import java.util.*;

//__________________________________________________________________________________________________
public class VRouterProtocol implements Cloneable, CDProtocol {

	// VARIABLE PARAMETERS
	final String PAR_K = "K";  // K值参数，表示路由表中存储的最大节点数
	final String PAR_ALPHA = "ALPHA";  // ALPHA值参数，表示每次查询时需要发送的最大消息数
	final String PAR_BITS = "BITS";  // BITS参数，用于位运算
	final String PAR_EXPECTED_ELEMENTS = "EXPECTED_ELEMENTS";  // 预计的元素数量
	final String FALSE_POSITIVE_PROB = "FALSE_POSITIVE_PROB";  // 假阳性概率
	private static String prefix = null;  // 配置前缀
	private int vRouterID;  // 虚拟路由器ID

	public Queue<VLookupMessage> lookupMessages;  // 存储查找消息的队列
	public Queue<IndexMessage> indexMessages;  // 存储索引消息的队列
	public HashMap<BigInteger, Integer> dataStorage;  // 存储数据的映射，key为数据ID，value为存储标识

	public HashMap<BigInteger, Integer> handledIndex = new HashMap<>();  // 记录已经处理过的索引消息
	public HashMap<BigInteger, Integer> handledQuery = new HashMap<>();  // 记录已经处理过的查找消息

	private Integer accessCount;
	private HashMap<BigInteger, Integer> uniqueAccessNodes;
	private HashMap<BigInteger, Integer> dataAccessCount;
	private HashMap<BigInteger, Set<BigInteger>> dataAccessNode;
	private HashMap<String, Object> dataMetrics;	//评分依据

	/**
	 * allow to call the service initializer only once
	 */
	private static boolean _ALREADY_INSTALLED = false;  // 是否已安装标识

	/**
	 * nodeId of this pastry node
	 */
	public BigInteger nodeId;  // 节点的ID


	/**
	 * routing table of this pastry node
	 */
	public RoutingTable routingTable;  // 路由表

	/**
	 * routing table with BloomFilter
	 */
	public BloomFilterRoutingTable bfRoutingTable;  // 带布隆过滤器的路由表

	/**
	 * Replicate this object by returning an identical copy.<br>
	 * It is called by the initializer and do not fill any particular field.
	 *
	 * @return Object
	 */
	public Object clone() {
		VRouterProtocol dolly = new VRouterProtocol(VRouterProtocol.prefix);  // 创建VRouterProtocol的副本
		return dolly;
	}

	/**
	 * Used only by the initializer when creating the prototype. Every other instance call CLONE to create the new object.
	 *
	 * @param prefix
	 *            String
	 */
	public VRouterProtocol(String prefix) {
		this.nodeId = null;  // 初始化为空的nodeId
		VRouterProtocol.prefix = prefix;  // 设置配置前缀
		_init();  // 初始化配置
		routingTable = new RoutingTable();  // 初始化路由表
		bfRoutingTable = new BloomFilterRoutingTable();  // 初始化带布隆过滤器的路由表
		dataStorage = new HashMap<>();  // 初始化数据存储

		lookupMessages = new LinkedList<>();  // 初始化查找消息队列
		indexMessages = new LinkedList<>();  // 初始化索引消息队列

		accessCount=0;
		uniqueAccessNodes=new HashMap<>();
		dataAccessCount=new HashMap<>();
		dataAccessNode=new HashMap<>();
		dataMetrics = new HashMap<>();
	}


	/**
	 * This procedure is called only once and allow to initialize the internal state of KademliaProtocol.
	 * Every node shares the same configuration, so it is sufficient to call this routine once.
	 */
	private void _init() {
		// execute once
		if (_ALREADY_INSTALLED)  // 如果已经安装过配置，直接返回
			return;

		// 读取配置参数
		KademliaCommonConfig.K = Configuration.getInt(prefix + "." + PAR_K, KademliaCommonConfig.K);  // 读取K值
		KademliaCommonConfig.ALPHA = Configuration.getInt(prefix + "." + PAR_ALPHA, KademliaCommonConfig.ALPHA);  // 读取ALPHA值
		KademliaCommonConfig.BITS = Configuration.getInt(prefix + "." + PAR_BITS, KademliaCommonConfig.BITS);  // 读取BITS值
		VRouterCommonConfig.EXPECTED_ELEMENTS = Configuration.getInt(prefix + "." + PAR_EXPECTED_ELEMENTS, VRouterCommonConfig.EXPECTED_ELEMENTS);  // 读取预计元素数量
		VRouterCommonConfig.FALSE_POSITIVE_PROB = Configuration.getDouble(prefix + "." + FALSE_POSITIVE_PROB, VRouterCommonConfig.FALSE_POSITIVE_PROB);  // 读取假阳性概率

		_ALREADY_INSTALLED = true;  // 标记已安装配置
	}
	private void updateDataMetrics(BigInteger sourceNodeId, BigInteger dataId) {
		// 更新总访问次数
		accessCount+=1;
		// 更新独立访问节点数
		uniqueAccessNodes.put(sourceNodeId,1);
		// 更新数据访问次数
		dataAccessCount.put(dataId, dataAccessCount.getOrDefault(dataId, 0) + 1);
		dataAccessNode.computeIfAbsent(dataId, k -> new HashSet<>()).add(sourceNodeId);
		// System.out.println("Updated dataAccessNode for dataId: " + dataId + ", sourceNodeId: " + sourceNodeId);
		// 记录访问日志
		// dataAccessNode
		// 		.computeIfAbsent(dataId, k -> new HashSet<>())  // 如果数据ID不存在，创建新的Set
		// 		.add(sourceNodeId);
	}
	private void initDataMetrics()
	{
		accessCount=0;
		uniqueAccessNodes.clear();
		dataAccessCount.clear();
		dataAccessNode.clear();
	}

	/**
	 * 通过执行二分查找，在网络中搜索具有特定节点Id的节点（我们关注网络的排序）
	 *
	 * @param searchNodeId
	 *            BigInteger
	 * @return Node
	 */
	private Node nodeIdtoNode(BigInteger searchNodeId) {
		if (searchNodeId == null)  // 如果目标节点ID为空，返回null
			return null;

		int inf = 0;
		int sup = Network.size() - 1;
		int m;

		while (inf <= sup) {  // 使用二分查找节点
			m = (inf + sup) / 2;

			BigInteger mId = ((VRouterProtocol) Network.get(m).getProtocol(vRouterID)).nodeId;

			if (mId.equals(searchNodeId))  // 找到目标节点
				return Network.get(m);

			if (mId.compareTo(searchNodeId) < 0)  // 目标节点在中点右边
				inf = m + 1;
			else  // 目标节点在中点左边
				sup = m - 1;
		}

		// 如果没有找到，进行传统的线性查找，确保更高的可靠性
		BigInteger mId;
		for (int i = Network.size() - 1; i >= 0; i--) {  // 反向遍历
			mId = ((VRouterProtocol) Network.get(i).getProtocol(vRouterID)).nodeId;
			if (mId.equals(searchNodeId))  // 找到目标节点
				return Network.get(i);
		}

		return null;  // 如果没有找到目标节点，返回null
	}

	/**
	 * set the current NodeId
	 *
	 * @param tmp
	 *            BigInteger
	 */
	public void setNodeId(BigInteger tmp) {
		this.nodeId = tmp;  // 设置当前节点的ID
		this.routingTable.nodeId = tmp;  // 设置路由表的节点ID
	}

	@Override
	public void nextCycle(Node node, int protocolID) {
		MyNode myNode = (MyNode) node; // 将节点转换为 MyNode
		this.vRouterID = protocolID;  // 更新协议ID
		while (!lookupMessages.isEmpty()) {  // 处理查找消息队列中的所有消息
			VLookupMessage msg = lookupMessages.poll();
			if (msg == null) continue;
			routingTable.addNeighbour(msg.from);  // 将消息发送方添加到路由表
			if (msg.dataID.equals(QueryGenerator.DEBUGTARGET)) {  // 调试时记录查询路径
				QueryGenerator.queryPath.add(Util.distance(msg.dataID, this.nodeId));
			}
			handleLookupMessage(msg, protocolID);  // 处理查找消息
		}
		while (!indexMessages.isEmpty()) {  // 处理索引消息队列中的所有消息
			IndexMessage msg = indexMessages.poll();
			if (msg == null) continue;
			routingTable.addNeighbour(msg.from);  // 将消息发送方添加到路由表
			if (msg.dataID.equals(QueryGenerator.DEBUGTARGET)) {  // 调试时记录索引路径
				QueryGenerator.indexPath.add(Util.distance(msg.dataID, this.nodeId));
			}
			handleIndexMessage(msg, protocolID);  // 处理索引消息
		}

		long currentCycle = peersim.core.CommonState.getTime();

		// 如果是中心节点，执行中心节点任务
		if (myNode.getIsCentralNode(currentCycle) && QueryGenerator.executeFlag) {
		System.out.println("VRouterProtocol nextCycle called for node: " + myNode.getID()+" isCentral: "+myNode.getIsCentralNode(currentCycle));
			myNode.getCentralNodeManager().execute();
		}
	}
	/**
	 * Handles the lookup message and performs the necessary actions to find the target data.
	 * @param msg The lookup message to handle.
	 * @param protocolID The protocol ID.
	 */
	public void handleLookupMessage(VLookupMessage msg, int protocolID) {
		// 统计数据查询的消息数
		if (VRouterObserver.dataQueryTraffic.get(msg.dataID) != null) {
			int msgs = VRouterObserver.dataQueryTraffic.get(msg.dataID);
			msgs++;
			VRouterObserver.dataQueryTraffic.put(msg.dataID, msgs);
		}

		// 避免本节点重复请求同一个数据，默认请求只发送一次。
		if (QueryGenerator.queriedData.get(msg.dataID) == 1) {
			// VRouterObserver.droppedLookupMessage.add(msg.forwardHops + msg.backwardHops);
			return;
		}

		// 如果消息已经处理过，直接返回
		if (handledQuery.containsKey(msg.dataID)) return;

		updateDataMetrics(msg.from, msg.dataID);

		// 如果本地存储了数据
		if (dataStorage.containsKey(msg.dataID)) {
			QueryGenerator.queriedData.put(msg.dataID, 1);
			VRouterObserver.successLookupForwardHop.add(msg.forwardHops);  // 记录查找成功的前向跳数
			VRouterObserver.successLookupBackwardHop.add(msg.backwardHops);  // 记录查找成功的后向跳数
			VRouterObserver.totalSuccessHops.add(msg.forwardHops + msg.backwardHops);  // 记录查找成功的总跳数
			return;
		}

		// 查找目标数据在反向路由表中的匹配节点
		List<BigInteger> backwardList = bfRoutingTable.getMatch(msg.dataID);
		if (backwardList != null) {
			// 从反向列表中去除距离目标数据ID更近的节点
			backwardList.removeIf(n -> Util.distance(n, msg.dataID).compareTo(Util.distance(this.nodeId, msg.dataID)) < 0);

			// 如果反向路由表中找到了匹配节点
			if (backwardList.size() > 0) {
				// 向目标数据的父节点发出反向查询请求
				for (BigInteger n : backwardList) {
					Node nextHop = this.nodeIdtoNode(n);  // 找到下一跳节点
					VRouterProtocol nextProtocol = (VRouterProtocol) nextHop.getProtocol(protocolID);  // 获取该节点的协议实例
					VLookupMessage nextMsg = msg.backward(this.nodeId);  // 创建反向消息
					nextProtocol.lookupMessages.add(nextMsg);  // 将反向消息加入下一跳节点的查找消息队列
				}
			}
		}

		// 如果消息是查找消息，才需要继续转发
		if (msg.direction) {
			VLookupMessage nextHop = msg.forward(this.nodeId);  // 创建转发消息
			BigInteger[] closerNodes = getCloserNodes(nextHop.dataID);  // 获取离目标数据ID更近的节点

			// 将消息发送给更接近的节点
			for (int i = 0; i < closerNodes.length; i++) {
				Node targetNode = this.nodeIdtoNode(closerNodes[i]);  // 找到目标节点
				VRouterProtocol targetPro = (VRouterProtocol) targetNode.getProtocol(protocolID);  // 获取该节点的协议实例
				targetPro.lookupMessages.add(nextHop);  // 将转发消息加入目标节点的查找消息队列
				// VRouterObserver.totalIndexHop.add(1);  // 记录消息转发次数（可选）
			}
		}

		handledQuery.put(msg.dataID, 1);  // 标记该数据ID的查找已处理
		updateDataMetrics(msg.from, msg.dataID);
	}

	/**
	 * Handles the index message and performs the necessary actions to index the target data.
	 * @param msg The index message to handle.
	 * @param protocolID The protocol ID.
	 */
	public void handleIndexMessage(IndexMessage msg, int protocolID) {
		this.vRouterID = protocolID;  // 更新协议ID

		// 统计数据索引的消息数
		if (VRouterObserver.dataIndexTraffic.get(msg.dataID) != null) {
			int msgs = VRouterObserver.dataIndexTraffic.get(msg.dataID);
			msgs++;
			VRouterObserver.dataIndexTraffic.put(msg.dataID, msgs);
		}

		// 查找目标节点的布隆过滤器联系
		ContactWithBloomFilter bfContact = this.bfRoutingTable.get(msg.from);
		if (bfContact == null) {
			bfContact = new ContactWithBloomFilter(msg.from);  // 如果没有找到联系，创建新的布隆过滤器联系
			this.bfRoutingTable.put(bfContact);  // 将联系添加到布隆过滤器路由表中
		}
		bfContact.add(msg.dataID);  // 将目标数据ID添加到布隆过滤器中

		// 如果该索引消息已处理，返回
		if (handledIndex.containsKey(msg.dataID)) {
			return;
		}

		IndexMessage relay = msg.relay(this.nodeId);  // 创建索引消息的中继消息
		BigInteger[] closerNodes = getCloserNodes(msg.dataID);  // 获取离目标数据ID更接近的节点

		// 将中继消息发送给更接近的节点
		for (BigInteger closerNode : closerNodes) {
			Node targetNode = this.nodeIdtoNode(closerNode);  // 找到目标节点
			VRouterProtocol targetPro = (VRouterProtocol) targetNode.getProtocol(protocolID);  // 获取该节点的协议实例
			targetPro.indexMessages.add(relay);  // 将中继消息加入目标节点的索引消息队列
			if (msg.dataID.equals(QueryGenerator.DEBUGTARGET)) {
				QueryGenerator.indexToPath.add(Util.distance(msg.dataID, targetPro.nodeId));  // 调试时记录索引路径
			}
			// VRouterObserver.totalIndexHop.add(1);  // 记录消息转发次数（可选）
		}

		// 如果没有更接近的节点，说明本地节点是最接近的
		if (closerNodes.length == 0) {
			VRouterObserver.indexHop.add(msg.hops);  // 记录索引消息的跳数
		}

		handledIndex.put(msg.dataID, 1);  // 标记该数据ID的索引已处理
	}

	/**
	 * Store data locally and propagate the index message to closer nodes.
	 * @param dataID The data ID to store.
	 * @param protocolID The protocol ID.
	 */
	public void storeData(BigInteger dataID, int protocolID) {
		dataStorage.put(dataID, 0);  // 将数据ID存储到本地数据存储中
		IndexMessage msg = new IndexMessage(dataID, this.nodeId);  // 创建索引消息
		BigInteger[] closerNodes = getCloserNodes(msg.dataID);  // 获取离目标数据ID更接近的节点

		// 将索引消息发送给更接近的节点
		for (int i = 0; i < closerNodes.length; i++) {
			Node targetNode = this.nodeIdtoNode(closerNodes[i]);  // 找到目标节点
			VRouterProtocol targetPro = (VRouterProtocol) targetNode.getProtocol(protocolID);  // 获取该节点的协议实例
			targetPro.indexMessages.add(msg);  // 将索引消息加入目标节点的索引消息队列
		}
	}

	/**
	 * Get the closer nodes to the target dataID.
	 * @param targetID The target data ID to find closer nodes.
	 * @return An array of closer node IDs.
	 */
	public BigInteger[] getCloserNodes(BigInteger targetID) {
		BigInteger[] neighbors = routingTable.getNeighbours(targetID);  // 获取目标数据ID的邻居节点

		int targetFlag = 0;
		for (int i = 0; i < neighbors.length; i++) {
			// 如果目标数据ID距离本地节点更远，停止遍历
			if (Util.distance(neighbors[i], targetID).compareTo(Util.distance(this.nodeId, targetID)) >= 0) break;
			targetFlag++;
		}

		// 选择距离目标数据ID更接近的节点
		BigInteger[] closerNodes = new BigInteger[Math.min(targetFlag, KademliaCommonConfig.ALPHA)];
		// 复制邻居节点
		for (int i = 0; i < Math.min(targetFlag, KademliaCommonConfig.ALPHA); i++) {
			closerNodes[i] = neighbors[i];
		}
		return closerNodes;  // 返回更接近的节点
	}
	public HashMap<String, Object> getDataMetrics() {
		HashMap<String, Object> metrics = new HashMap<>();
		metrics.put("accessCount", accessCount);
		metrics.put("uniqueAccessNodes", uniqueAccessNodes.size());
		metrics.put("dataAccessCount", new HashMap<>(dataAccessCount));
		metrics.put("dataAccessNode", new HashMap<>(dataAccessNode));
		initDataMetrics();
		return metrics;
	}
}