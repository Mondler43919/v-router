package vRouter;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BloomFilterRoutingTable {

    // 用于存储节点及其对应的布隆过滤器信息，键为节点ID，值为包含布隆过滤器的联系对象
    HashMap<BigInteger, ContactWithBloomFilter> bfRoutingTable;

    // 向路由表中添加一个联系对象，包含该节点的布隆过滤器信息
    public void put(ContactWithBloomFilter contactBF){
        // 如果路由表为空，初始化它
        if(bfRoutingTable == null){
            bfRoutingTable = new HashMap<>();
        }
        // 将联系对象与其对应的节点ID（contact）添加到路由表中
        bfRoutingTable.put(contactBF.contact, contactBF);
    }

    // 根据节点ID获取对应的布隆过滤器信息
    public ContactWithBloomFilter get(BigInteger node){
        // 如果路由表为空，返回null
        if(bfRoutingTable == null){
            return null;
        }
        // 返回对应节点ID的布隆过滤器信息
        return bfRoutingTable.get(node);
    }

    // 根据数据ID查找匹配该数据的节点ID列表
    public List<BigInteger> getMatch(BigInteger dataID){
        // 如果路由表为空，返回null
        if(bfRoutingTable == null){
            return null;
        }
        // 存储所有匹配的节点ID
        List<BigInteger> matchNodes = new ArrayList<>();

        // 遍历路由表中的每个联系对象，检查该节点的布隆过滤器是否包含目标数据ID
        for (ContactWithBloomFilter c: bfRoutingTable.values()) {
            // 如果布隆过滤器包含目标数据ID，则将节点ID加入匹配节点列表
            if(c.contain(dataID)) {
                matchNodes.add(c.contact);
            }
        }
        // 返回所有匹配的节点ID
        return matchNodes;
    }
}
