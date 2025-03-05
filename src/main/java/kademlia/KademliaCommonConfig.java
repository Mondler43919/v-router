package kademlia;

/**
 * Kademlia 网络的固定参数配置类。这些参数有默认值，并且可以在网络启动时一次性配置。
 *
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */
public class KademliaCommonConfig {

	// 网络中节点 ID 的长度（默认是 160 位）
	public static int BITS = 160;

	// k-buckets 的维度（默认是 20）
	public static int K = 20;

	// 同时进行查找的数量（默认是 3）
	public static int ALPHA = 3;

	/**
	 * 返回当前 Kademlia 配置的简要信息
	 *
	 * @return 返回一个包含 K、ALPHA 和 BITS 配置的字符串
	 */
	public static String info() {
		return String.format("[K=%d][ALPHA=%d][BITS=%d]", K, ALPHA, BITS);
	}

}
