package vRouter;

import orestes.bloomfilter.BloomFilter;
import orestes.bloomfilter.FilterBuilder;
import orestes.bloomfilter.HashProvider;
import orestes.bloomfilter.memory.BloomFilterMemory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * 代表一个节点及其与多个布隆过滤器的关联。
 * 每个 ContactWithBloomFilter 对象存储一个联系人的数据并使用布隆过滤器来检查数据ID的存在性。
 */
public class ContactWithBloomFilter {

    // 节点的标识（即该节点的 contact ID）
    final BigInteger contact;

    // 存储与节点相关联的布隆过滤器列表
    List<BloomFilter<BigInteger>> bloomFilterList;

    /**
     * 构造函数，初始化节点的 contact ID
     *
     * @param income 节点的 contact ID
     */
    public ContactWithBloomFilter(BigInteger income) {
        contact = income;
    }

    /**
     * 向当前节点关联的布隆过滤器列表中添加数据ID。
     * 如果数据ID已经存在于某个过滤器中，则不再添加。
     * 如果没有合适的空间，则创建新的布隆过滤器并添加数据。
     *
     * @param dataID 要存储的数据ID
     */
    public void add(BigInteger dataID) {
        // 如果没有布隆过滤器列表，则初始化一个新的列表
        if (bloomFilterList == null) {
            bloomFilterList = new ArrayList<>();
        }

        // 如果数据ID已经存在于过滤器中，则直接返回
        if (contain(dataID)) return;

        // 遍历现有的所有布隆过滤器，找到一个没有达到预期元素数的过滤器
        for (BloomFilter<BigInteger> bf : bloomFilterList) {
            if (bf.getEstimatedPopulation() < bf.getExpectedElements()) {
                bf.add(dataID);  // 将数据ID添加到该布隆过滤器
                return;
            }
        }

        // 如果没有找到合适的过滤器，创建一个新的布隆过滤器并添加数据ID
        BloomFilter<BigInteger> bf = new BloomFilterMemory<>(
                new FilterBuilder(VRouterCommonConfig.EXPECTED_ELEMENTS, VRouterCommonConfig.FALSE_POSITIVE_PROB)
                        .hashFunction(HashProvider.HashMethod.MD5).complete()
        );
        bf.add(dataID);

        // 更新布隆过滤器的计数
        VRouterObserver.bloomFilterCount.add(1);

        // 将新创建的布隆过滤器添加到列表中
        bloomFilterList.add(bf);
    }

    /**
     * 检查数据ID是否存在于当前节点的任何布隆过滤器中。
     *
     * @param dataID 要检查的数据ID
     * @return 如果数据ID在任何布隆过滤器中存在，则返回 true；否则返回 false。
     */
    public boolean contain(BigInteger dataID) {
        // 如果布隆过滤器列表为空，则返回 false
        if (bloomFilterList == null) {
            return false;
        }

        // 遍历所有的布隆过滤器，检查是否包含该数据ID
        for (BloomFilter<BigInteger> bf : bloomFilterList) {
            if (bf.contains(dataID)) return true;  // 如果某个过滤器包含该数据ID，返回 true
        }

        // 如果没有任何过滤器包含该数据ID，返回 false
        return false;
    }
}
