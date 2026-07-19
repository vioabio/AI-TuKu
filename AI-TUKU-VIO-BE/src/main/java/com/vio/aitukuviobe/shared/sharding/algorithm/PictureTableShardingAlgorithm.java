package com.vio.aitukuviobe.shared.sharding.algorithm;

import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * Picture 表按 spaceId 分片算法
 * <p>
 * 分片策略：
 * <ul>
 *   <li>spaceId 为 null（公共图库） → picture_0</li>
 *   <li>spaceId 非空 → picture_{spaceId % 4 + 1}（共 4 片：picture_1 ~ picture_4）</li>
 * </ul>
 * <p>
 * 使用方式：在 ShardingSphere YAML 配置中指定此算法类名：
 * <pre>
 *   picture-sharding-algorithm:
 *     type: CLASS_BASED
 *     props:
 *       strategy: standard
 *       algorithmClassName: com.vio.aitukuviobe.shared.sharding.algorithm.PictureTableShardingAlgorithm
 * </pre>
 *
 * @author vivin
 */
public class PictureTableShardingAlgorithm implements StandardShardingAlgorithm<Comparable<?>> {

    /**
     * 默认分片数（spaceId 非 null 时的分片数）
     */
    private static final int SHARD_COUNT = 4;

    /**
     * 默认表名前缀
     */
    private static final String TABLE_PREFIX = "picture_";

    @Override
    public String doSharding(Collection<String> availableTargetNames,
                             PreciseShardingValue<Comparable<?>> shardingValue) {
        Comparable<?> value = shardingValue.getValue();
        String actualTableName;

        if (value == null) {
            // spaceId 为 null → 路由到默认表 picture_0
            actualTableName = TABLE_PREFIX + 0;
        } else {
            long spaceId = ((Number) value).longValue();
            long shardIndex = (spaceId % SHARD_COUNT) + 1;
            actualTableName = TABLE_PREFIX + shardIndex;
        }

        // 验证目标表是否在可用列表中
        if (availableTargetNames.contains(actualTableName)) {
            return actualTableName;
        }

        // 如果计算出的表不在列表（例如刚新增的分片），回退到 picture_0
        return TABLE_PREFIX + 0;
    }

    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames,
                                         RangeShardingValue<Comparable<?>> shardingValue) {
        // 范围查询需要广播到所有分片
        return new LinkedHashSet<>(availableTargetNames);
    }

    @Override
    public String getType() {
        return "PICTURE_SPACE_ID_SHARDING";
    }
}
