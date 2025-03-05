package vRouter;

import peersim.core.Network;
import java.math.BigInteger;
import java.util.*;

public class CentralNodeManager {
    private BigInteger currentCentralNode;
    private MyNode newCentralNode;
    private List<BigInteger> candidateList;
    private MyNode currentNode; // 当前节点的引用
    private List<String> dataHashes; // 存储所有节点的数据哈希值
    private HashMap<BigInteger, Double> nodeScores;  // 记录节点评分
    private HashMap<BigInteger, Object> nodeMetrics;  // 存储每个节点的评分依据
    private HashMap<BigInteger, Double> dataScores;  // 记录数据评分
    private HashMap<BigInteger, Set<BigInteger>> dataAccessNodes; //数据在全网被访问的节点
    private HashMap<BigInteger, Integer> dataAccessCounts; // 数据在全网被访问次数

    // 构造方法
    public CentralNodeManager(MyNode currentNode) {
        this.currentNode = currentNode; // 传入当前节点的引用
        nodeScores = new HashMap<>();
        dataScores = new HashMap<>();
        dataAccessNodes = new HashMap<>();
        dataAccessCounts = new HashMap<>();
        candidateList = new LinkedList<>();
        dataHashes = new ArrayList<>();
        nodeMetrics = new HashMap<>();
    }

    // 合并全网独立访问节点 ID 并统计访问次数
    private void mergeGlobalDataAccessNodes(HashMap<String, Object> nodeDataMetrics) {

        // 获取当前节点的数据访问记录
        HashMap<BigInteger, Set<BigInteger>> dataAccessNode =
                (HashMap<BigInteger, Set<BigInteger>>) nodeDataMetrics.get("dataAccessNode");
        if (dataAccessNode == null) {
            System.out.println("dataAccessNode is null");
            System.out.println(nodeDataMetrics.keySet());
            return;
        }

        // 遍历当前节点的数据访问记录
        for (Map.Entry<BigInteger, Set<BigInteger>> entry : dataAccessNode.entrySet()) {
            BigInteger dataId = entry.getKey();
            Set<BigInteger> accessNodes = entry.getValue();

            // 如果全局记录中还没有该数据 ID，初始化一个新的 Set
            dataAccessNodes.computeIfAbsent(dataId, k -> new HashSet<>()).addAll(accessNodes);

            // 更新该数据的访问次数
            dataAccessCounts.put(dataId, dataAccessCounts.getOrDefault(dataId, 0) + accessNodes.size());
        }
    }

    // 向所有节点请求评分数据
    public void getData() {
        for (int i = 0; i < Network.size(); i++) {
            MyNode node = (MyNode) Network.get(i);

            double score = node.getNodeScore();
            nodeScores.put(node.nodeId, score);

            // 各节点数据
            HashMap<String, Object> data = node.getDataMetrics();
            String dataHash = node.calculateDataHash(data);
            dataHashes.add(dataHash);
            // 数据评分依据
            mergeGlobalDataAccessNodes(data);
            // 节点评分依据
            data.remove("dataAccessNode");
            nodeMetrics.put(node.nodeId, data);
        }
        System.out.println("信息收集完成" );
    }

    public void getNewCentralNode() {
        List<Map.Entry<BigInteger, Double>> sortedScores = new ArrayList<>(nodeScores.entrySet());
        sortedScores.sort((entry1, entry2) -> Double.compare(entry2.getValue(), entry1.getValue()));  // 降序排序

        // 假设前 N 个节点为候选节点
        int N = 5;
        for (int i = 0; i < N && i < sortedScores.size(); i++) {
            BigInteger nodeId = sortedScores.get(i).getKey();
            candidateList.add(nodeId);
        }
        Collections.shuffle(candidateList);
        if (candidateList.isEmpty()) {
            System.out.println("没有候选节点，无法更新中心节点。");
            return;
        }
        newCentralNode = VRFElection.electNextCentralNode(candidateList);
    }

    // 计算数据评分
    public void calculateAndUploadScores() {
        dataScores = DataActivityScore.calculateActivityScore(dataAccessCounts, dataAccessNodes);
<<<<<<< HEAD
        System.out.println("已计算数据评分 ");
=======
        System.out.println("已计算数据评分 "+dataScores);
>>>>>>> 22f23f5 (添加了活跃度评分)
    }

    private void initCentralNodeManager() {
        nodeScores.clear();
        dataScores.clear();
        candidateList.clear();
        dataAccessNodes.clear();
        dataAccessCounts.clear();
    }

    // 生成默克尔树并上传数据到区块链
    public void generateAndUpload() {
        // 生成默克尔树
        MerkleTree merkleTree = new MerkleTree(dataHashes);
        String rootHash = merkleTree.getRootHash();

        // 获取当前节点的区块链
        Blockchain blockchain = currentNode.getBlockchain();
        Block block = blockchain.packageData(rootHash, newCentralNode.nodeId, candidateList,
                dataScores, dataAccessCounts, dataAccessNodes,
                nodeScores, nodeMetrics);
        blockchain.broadcastBlock(block);
        System.out.println("上传成功" );
    }

    // 主执行流程
    public void execute() {
        getData();
        getNewCentralNode();
        calculateAndUploadScores();
        generateAndUpload();
        initCentralNodeManager();
        if (newCentralNode != null && !(newCentralNode.nodeId).equals(currentCentralNode)) {
            long currentCycle = peersim.core.CommonState.getTime();
            newCentralNode.setAsCentralNode(true,currentCycle+1);
            currentNode.setAsCentralNode(false,currentCycle);
            System.out.println("选举出新中心节点: " + newCentralNode.getID());
        }
    }
}