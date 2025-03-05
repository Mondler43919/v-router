package vRouter;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DataActivityScore {

    // 静态方法：计算所有数据的活跃度评分
    public static HashMap<BigInteger, Double> calculateActivityScore(
            HashMap<BigInteger, Integer> dataAccessCounts,  // 数据访问次数
            HashMap<BigInteger, Set<BigInteger>> dataAccessNodes,  // 数据访问的独立节点
            double weightAccessCount,  // 访问次数的权重系数
            double weightUniqueAccessNodes) {  // 独立访问节点数的权重系数

        // 参数校验
        if (dataAccessCounts == null || dataAccessNodes == null) {
            throw new IllegalArgumentException("输入的参数不能为空");
        }

        if (weightAccessCount < 0 || weightUniqueAccessNodes < 0) {
            throw new IllegalArgumentException("权重系数不能为负");
        }

        // 活跃度评分结果
        HashMap<BigInteger, Double> activityScores = new HashMap<>();

        // 遍历所有数据
        for (Map.Entry<BigInteger, Integer> entry : dataAccessCounts.entrySet()) {
            BigInteger dataId = entry.getKey();
            int accessCount = entry.getValue();

            // 获取独立访问节点的数量，避免 null 值
            Set<BigInteger> nodes = dataAccessNodes.get(dataId);
            int uniqueAccessNodes = (nodes == null) ? 0 : nodes.size();

            // 计算活跃度评分：权重 + 加权计算
            double score = weightAccessCount * accessCount + weightUniqueAccessNodes * uniqueAccessNodes;
            activityScores.put(dataId, score);
        }

        return activityScores;
    }

    // 重载方法：使用默认的权重系数
    public static HashMap<BigInteger, Double> calculateActivityScore(
            HashMap<BigInteger, Integer> dataAccessCounts,
            HashMap<BigInteger, Set<BigInteger>> dataAccessNodes) {

        // 默认权重系数
        double defaultWeightAccessCount = 0.6;
        double defaultWeightUniqueAccessNodes = 0.4;

        return calculateActivityScore(dataAccessCounts, dataAccessNodes, defaultWeightAccessCount, defaultWeightUniqueAccessNodes);
    }
}
