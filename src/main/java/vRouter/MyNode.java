package vRouter;

import peersim.core.GeneralNode;
import peersim.core.Network;
import peersim.config.Configuration;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MyNode extends GeneralNode{
    private final static String PAR_PROT = "protocol";
    private final int pid;
    private static String prefix ="vRouter";
    public boolean alreadyUpdate;
    private boolean[] isCentralNode;
    private double nodeScore;
    private HashMap<BigInteger, Double> dataScore;  // 记录数据评分
    private HashMap<String, Object> dataMetrics;    //评分依据

    public BigInteger nodeId;
    private Blockchain blockchain;
    private CentralNodeManager centralNodeManager;

    public MyNode(String prefix) {
        super(prefix);  // 调用 GeneralNode 的构造方法
        pid = Configuration.getPid(prefix + "." + PAR_PROT);
        isCentralNode = new boolean[5];

        nodeScore = 0.0;
        dataScore = new HashMap<>();
        dataMetrics = new HashMap<>();

        blockchain = new Blockchain();
        centralNodeManager = new CentralNodeManager(this);
        alreadyUpdate=false;
    }
    @Override
    public MyNode clone() {
        MyNode clone = (MyNode) super.clone();  // 调用父类的 clone 方法
        // 深拷贝需要独立的对象
        clone.dataScore = new HashMap<>(this.dataScore);  // 深拷贝 dataScore
        clone.dataMetrics = new HashMap<>(this.dataMetrics);  // 深拷贝 dataMetrics
        clone.blockchain = new Blockchain();  // 每个节点拥有独立的区块链
        clone.centralNodeManager = new CentralNodeManager(clone);  // 深拷贝 centralNodeManager
        return clone;
    }
    // 同步区块链
    public void syncBlockchain() {
        // 从网络中获取最新的区块链
        for (int i = 0; i < Network.size(); i++) {
            MyNode otherNode = (MyNode) Network.get(i);
            if (otherNode != this) {
                Blockchain otherBlockchain = otherNode.getBlockchain();
                if (otherBlockchain.getChain().size() > blockchain.getChain().size()) {
                    // 如果其他节点的区块链更长，则替换本地区块链
                    blockchain = otherBlockchain;
                    System.out.println("节点 " + nodeId + " 同步区块链");
                }
            }
        }
    }
    public void updateData() {
        VRouterProtocol p = (VRouterProtocol) this.getProtocol(pid);
        this.dataMetrics =p.getDataMetrics();
        this.nodeScore = NodeActivityScore.calculateActivityScore(dataMetrics);
        alreadyUpdate=true;
    }

    // 计算数据的哈希值
    public String calculateDataHash(HashMap<String, Object> dataMetrics) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String dataString = dataMetrics.toString(); // 将数据转换为字符串
            byte[] hashBytes = digest.digest(dataString.getBytes());
            return bytesToHex(hashBytes); // 将字节数组转换为十六进制字符串
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 算法不可用", e);
        }
    }
    // 将字节数组转换为十六进制字符串
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public void receiveBlock(Block block) {
        // 验证区块的有效性
        if (isBlockValid(block)) {
            // 如果区块有效，添加到本地区块链
            blockchain.addBlock(block);
            //更新数据评分
            setDataScore(((BlockData)block.getData()).getDataScores());
//            System.out.println("节点 " + nodeId + " 接收到新区块: " + block.getBlockHash());
        } else {
            System.out.println("节点 " + nodeId + " 接收到无效区块: " + block.getBlockHash());
        }
    }
    // 验证区块的有效性
    private boolean isBlockValid(Block block) {
        // System.out.println("区块哈希："+ block.getBlockHash() + "计算哈希" + block.calculateHash());
        // 验证区块的哈希是否正确
        if (!block.getBlockHash().equals(block.calculateHash())) {
            System.out.println("区块哈希不对");
            return false;
        }

        // 验证区块的前一个哈希是否与本地链的最后一个区块哈希匹配
        Block lastBlock = blockchain.getLastBlock();
        // System.out.println("thisblockPreviousHash："+ block.getPreviousHash());
        // System.out.println("chainlastBlockHash：" + lastBlock.getBlockHash());
        if (!block.getPreviousHash().equals(lastBlock.getBlockHash())) {
            return false;
        }
        return true;
    }

    public void setNodeId(BigInteger tmp) {
        nodeId = tmp;
    }
    public void setDataScore(HashMap<BigInteger, Double> dataScore) {
        this.dataScore = dataScore;
    }
    public void setAsCentralNode(boolean isCentralNode, long cycle) {
        int index = (int) (cycle % 5);  // 计算索引
        this.isCentralNode[index] = isCentralNode;
    }

    // 获取本地区块链
    public Blockchain getBlockchain() {
        return blockchain;
    }
    public HashMap<String, Object> getDataMetrics(){
        if(!alreadyUpdate) {
            updateData();
            alreadyUpdate=false;
        }
        return dataMetrics;
    }
    public double getNodeScore() {
        return nodeScore;
    }
    public Map<BigInteger, Double> getDataScore() {
        return dataScore;
    }
    public boolean getIsCentralNode(long cycle) {
        int index = (int) (cycle % 5);  // 计算索引
        return this.isCentralNode[index];
    }
    public CentralNodeManager getCentralNodeManager() {
        return centralNodeManager;
    }
}

