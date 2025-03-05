package vRouter;

import java.math.BigInteger;

/**
 * 表示一个查找消息 (Lookup Message) 的类，用于在节点之间传递查找请求。
 */
public class VLookupMessage {

    // 要查找的数据 ID
    public BigInteger dataID;

    // 消息的来源节点 ID
    public BigInteger from;

    // 向前查找的跳数统计
    public int forwardHops;

    // 向后查找的跳数统计
    public int backwardHops;

    // 消息方向：true 表示向前；false 表示向后
    public boolean direction;

    /**
     * 构造函数，初始化一个查找消息，设置初始方向为向前。
     *
     * @param t    要查找的数据 ID
     * @param from 消息的来源节点 ID
     */
    public VLookupMessage(BigInteger t, BigInteger from) {
        dataID = t;                 // 要查找的数据 ID
        this.from = from;           // 来源节点 ID
        forwardHops = 1;            // 初始向前跳数设为 1
        backwardHops = 0;           // 初始向后跳数设为 0
        direction = true;           // 初始方向为向前
    }

    /**
     * 创建一个新的向前查找的消息副本，并增加跳数。
     *
     * @param from 当前发送消息的节点 ID
     * @return 新的向前查找消息
     */
    public VLookupMessage forward(BigInteger from) {
        VLookupMessage msg = new VLookupMessage(this.dataID, from);
        msg.forwardHops = this.forwardHops + 1;  // 跳数加 1
        return msg;
    }

    /**
     * 创建一个新的向后查找的消息副本，并增加跳数，设置方向为向后。
     *
     * @param from 当前发送消息的节点 ID
     * @return 新的向后查找消息
     */
    public VLookupMessage backward(BigInteger from) {
        VLookupMessage msg = new VLookupMessage(this.dataID, from);
        msg.backwardHops = this.backwardHops + 1;  // 跳数加 1
        msg.direction = false;                     // 设置方向为向后
        return msg;
    }
}
