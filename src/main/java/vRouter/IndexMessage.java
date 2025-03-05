package vRouter;

import java.math.BigInteger;

public class IndexMessage {
    // 数据ID
    public BigInteger dataID;
    // 消息的来源节点ID
    public BigInteger from;
    // 消息传递的跳数
    public int hops;

    // 构造函数，初始化数据ID、来源节点和跳数
    public IndexMessage(BigInteger data, BigInteger origin){
        this.dataID = data;  // 设置数据ID
        this.from = origin;   // 设置来源节点ID
        this.hops = 1;        // 初始化跳数为1
    }

    // 生成一个转发消息的副本，更新跳数
    public IndexMessage relay(BigInteger local){
        // 创建一个新的IndexMessage副本，并设置新的来源节点为当前节点
        IndexMessage relay = new IndexMessage(dataID, local);
        relay.hops = this.hops + 1; // 跳数加1，表示消息已经转发了一次
        return relay; // 返回转发后的消息
    }
}
