package com.vio.aitukuviobe.infrastructure.monitoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 数据库索引验证器 — 应用启动时检查关键索引是否存在
 *
 * <p>对应优化文档 8.1 节：索引优化
 *
 * <p>检查项目：
 * <ul>
 *   <li>picture 表: (spaceId, reviewStatus) 复合索引</li>
 *   <li>picture 表: (userId, createTime) 复合索引</li>
 *   <li>space_user 表: (spaceId, userId) 唯一复合索引</li>
 * </ul>
 *
 * <p>缺失索引时打印 WARN 日志 + 提供修复 SQL，不阻塞启动。
 * 可通过 {@code aituku.index.check.enabled=false} 关闭检查。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aituku.index.check.enabled", havingValue = "true", matchIfMissing = true)
public class IndexValidationRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 关键索引定义
     */
    private static final List<IndexDefinition> REQUIRED_INDEXES = Arrays.asList(
        new IndexDefinition(
            "picture",
            "idx_spaceId_reviewStatus",
            Arrays.asList("spaceId", "reviewStatus"),
            "空间图片列表分页查询: SELECT * FROM picture WHERE spaceId=? AND reviewStatus=? AND isDelete=0"
        ),
        new IndexDefinition(
            "picture",
            "idx_userId_createTime",
            Arrays.asList("userId", "createTime"),
            "我的图片按时间排序: SELECT * FROM picture WHERE userId=? ORDER BY createTime DESC"
        ),
        new IndexDefinition(
            "space_user",
            "uk_spaceId_userId",
            Arrays.asList("spaceId", "userId"),
            "空间成员权限校验 + 防重复: SELECT * FROM space_user WHERE spaceId=? AND userId=?"
        )
    );

    @Override
    public void run(String... args) {
        log.info("========================================");
        log.info("  数据库索引健康检查");
        log.info("========================================");

        List<IndexDefinition> missingIndexes = new ArrayList<>();
        List<String> existingIndexNames = new ArrayList<>();

        for (IndexDefinition def : REQUIRED_INDEXES) {
            if (indexExists(def.tableName, def.indexName)) {
                existingIndexNames.add(def.tableName + "." + def.indexName);
            } else {
                missingIndexes.add(def);
            }
        }

        // 输出已存在索引
        if (!existingIndexNames.isEmpty()) {
            log.info("[OK] 已验证索引 ({}): {}", existingIndexNames.size(), String.join(", ", existingIndexNames));
        }

        // 输出缺失索引
        if (!missingIndexes.isEmpty()) {
            log.warn("========================================");
            log.warn("  [WARN] 发现 {} 个关键索引缺失!", missingIndexes.size());
            log.warn("========================================");
            for (IndexDefinition def : missingIndexes) {
                log.warn("  缺失索引: {}.{} ({})", def.tableName, def.indexName,
                    String.join(", ", def.columns));
                log.warn("      场景: {}", def.usageScenario);
                log.warn("      修复: {}", def.toCreateSql());
            }
            log.warn("  请执行 sql/index_optimization.sql 或运行以下 SQL:");
            for (IndexDefinition def : missingIndexes) {
                log.warn("    {}", def.toCreateSql());
            }
            log.warn("========================================");
        } else {
            log.info("[OK] 所有关键索引已就绪 ({} 项检查通过)", REQUIRED_INDEXES.size());
        }

        log.info("========================================");
    }

    /**
     * 判断指定表的索引是否存在
     */
    private boolean indexExists(String tableName, String indexName) {
        try {
            String sql = "SELECT COUNT(*) FROM information_schema.STATISTICS "
                + "WHERE TABLE_SCHEMA = DATABASE() "
                + "AND TABLE_NAME = ? "
                + "AND INDEX_NAME = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName, indexName);
            return count != null && count > 0;
        } catch (Exception e) {
            log.debug("检查索引 {}.{} 失败: {}", tableName, indexName, e.getMessage());
            return false;
        }
    }

    /**
     * 索引定义
     */
    private static class IndexDefinition {
        final String tableName;
        final String indexName;
        final List<String> columns;
        final String usageScenario;

        IndexDefinition(String tableName, String indexName, List<String> columns, String usageScenario) {
            this.tableName = tableName;
            this.indexName = indexName;
            this.columns = columns;
            this.usageScenario = usageScenario;
        }

        String toCreateSql() {
            boolean isUnique = indexName.startsWith("uk_");
            String type = isUnique ? "UNIQUE INDEX" : "INDEX";
            return String.format("ALTER TABLE %s ADD %s %s (%s);",
                tableName, type, indexName, String.join(", ", columns));
        }
    }
}
