package vRouter;

import java.util.UUID;

public class VRouterCommonConfig {

    // Bloom Filter Configuration
    public static int EXPECTED_ELEMENTS = 1000;  // 布隆过滤器预期元素数
    public static double FALSE_POSITIVE_PROB = 0.01;  // 布隆过滤器假阳性概率

    /**
     * 提供当前配置的简要信息
     *
     * @return 配置信息的字符串
     */
    public static String info() {
        return String.format(
                "[EXPECTED_ELEMENTS=%d][FALSE_POSITIVE_PROB=%f]",
                EXPECTED_ELEMENTS,
                FALSE_POSITIVE_PROB
        );
    }
}
