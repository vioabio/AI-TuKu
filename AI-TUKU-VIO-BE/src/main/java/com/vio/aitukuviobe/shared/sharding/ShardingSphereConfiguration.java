package com.vio.aitukuviobe.shared.sharding;

import com.vio.aitukuviobe.shared.sharding.algorithm.PictureTableShardingAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.driver.api.ShardingSphereDataSourceFactory;
import org.apache.shardingsphere.infra.config.algorithm.AlgorithmConfiguration;
import org.apache.shardingsphere.infra.config.mode.ModeConfiguration;
import org.apache.shardingsphere.readwritesplitting.api.ReadwriteSplittingRuleConfiguration;
import org.apache.shardingsphere.readwritesplitting.api.rule.ReadwriteSplittingDataSourceRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.StandardShardingStrategyConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.*;

/**
 * ShardingSphere 配置：读写分离 + Picture 表分片
 * <p>
 * 通过 spring.sharding.enabled=true 启用（默认关闭）：
 * <pre>
 * spring:
 *   profiles:
 *     include: sharding
 * </pre>
 * <p>
 * 分片规则：
 * <ul>
 *   <li>Picture 表按 spaceId 分 5 片（picture_0 存放公共图片，picture_1~4 存放私有空间图片）</li>
 *   <li>读写分离：写走 write_ds，读在 read_ds_0 / read_ds_1 间轮询</li>
 * </ul>
 *
 * @author vivin
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.sharding.enabled", havingValue = "true")
public class ShardingSphereConfiguration {

    @Value("${spring.sharding.write-ds.jdbc-url:jdbc:mysql://localhost:3306/vio_tuku}")
    private String writeJdbcUrl;

    @Value("${spring.sharding.write-ds.username:root}")
    private String writeUsername;

    @Value("${spring.sharding.write-ds.password:}")
    private String writePassword;

    @Value("${spring.sharding.read-ds-0.jdbc-url:jdbc:mysql://localhost:3307/vio_tuku}")
    private String read0JdbcUrl;

    @Value("${spring.sharding.read-ds-0.username:root}")
    private String read0Username;

    @Value("${spring.sharding.read-ds-0.password:}")
    private String read0Password;

    @Value("${spring.sharding.read-ds-1.jdbc-url:jdbc:mysql://localhost:3308/vio_tuku}")
    private String read1JdbcUrl;

    @Value("${spring.sharding.read-ds-1.username:root}")
    private String read1Username;

    @Value("${spring.sharding.read-ds-1.password:}")
    private String read1Password;

    /**
     * ShardingSphere 数据源（替代默认 DataSource）
     */
    @Bean
    @Primary
    public DataSource shardingSphereDataSource() throws SQLException {
        log.info("========================================");
        log.info("初始化 ShardingSphere 数据源...");
        log.info("  写库: {}", writeJdbcUrl);
        log.info("  读库0: {}", read0JdbcUrl);
        log.info("  读库1: {}", read1JdbcUrl);
        log.info("========================================");

        // 1. 配置物理数据源
        Map<String, DataSource> dataSourceMap = createDataSourceMap();

        // 2. 配置分片规则
        ShardingRuleConfiguration shardingRuleConfig = createShardingRule();

        // 3. 配置读写分离规则
        ReadwriteSplittingRuleConfiguration rwSplittingConfig = createReadwriteSplittingRule();

        // 4. 构建 ShardingSphere 数据源
        Properties props = new Properties();
        props.setProperty("sql-show", "false");
        props.setProperty("sql-simple", "true");

        return ShardingSphereDataSourceFactory.createDataSource(
                dataSourceMap,
                Arrays.asList(shardingRuleConfig, rwSplittingConfig),
                props
        );
    }

    /**
     * 创建物理数据源 Map
     */
    private Map<String, DataSource> createDataSourceMap() {
        Map<String, DataSource> dataSourceMap = new LinkedHashMap<>();

        // 写库（Master）
        dataSourceMap.put("write_ds", createHikariDataSource(
                "write_ds", writeJdbcUrl, writeUsername, writePassword));

        // 读库 0（Slave 0）
        dataSourceMap.put("read_ds_0", createHikariDataSource(
                "read_ds_0", read0JdbcUrl, read0Username, read0Password));

        // 读库 1（Slave 1）
        dataSourceMap.put("read_ds_1", createHikariDataSource(
                "read_ds_1", read1JdbcUrl, read1Username, read1Password));

        return dataSourceMap;
    }

    /**
     * 创建读/写分离规则
     */
    private ReadwriteSplittingRuleConfiguration createReadwriteSplittingRule() {
        // 读库负载均衡算法
        Properties loadBalancerProps = new Properties();
        loadBalancerProps.setProperty("type", "ROUND_ROBIN");

        AlgorithmConfiguration loadBalancerAlg = new AlgorithmConfiguration(
                "ROUND_ROBIN", loadBalancerProps);

        // 读写分离数据源规则
        ReadwriteSplittingDataSourceRuleConfiguration dsRule =
                new ReadwriteSplittingDataSourceRuleConfiguration(
                        "ms_ds",                          // 逻辑数据源名
                        "write_ds",                       // 写数据源
                        Arrays.asList("read_ds_0", "read_ds_1"),  // 读数据源列表
                        "round_robin");                    // 负载均衡器

        return new ReadwriteSplittingRuleConfiguration(
                Collections.singletonList(dsRule),
                Collections.singletonMap("round_robin", loadBalancerAlg));
    }

    /**
     * 创建分片规则（仅对 picture 表）
     */
    private ShardingRuleConfiguration createShardingRule() {
        ShardingRuleConfiguration config = new ShardingRuleConfiguration();

        // ---- Picture 表分片 ----
        // 逻辑表 picture → 物理表 picture_0 ~ picture_4
        // ms_ds 是读写分离的逻辑数据源，ShardingSphere 会自动路由读/写
        ShardingTableRuleConfiguration pictureTableRule =
                new ShardingTableRuleConfiguration("picture",
                        "ms_ds.picture_${0..4}");

        // 分片策略：按 space_id 列分片
        pictureTableRule.setTableShardingStrategy(
                new StandardShardingStrategyConfiguration("space_id",
                        "picture_sharding_alg"));

        config.getTables().add(pictureTableRule);

        // ---- 分片算法配置 ----
        Properties shardingProps = new Properties();
        AlgorithmConfiguration shardingAlg = new AlgorithmConfiguration(
                "CLASS_BASED", shardingProps);
        shardingProps.setProperty("strategy", "standard");
        shardingProps.setProperty("algorithmClassName",
                PictureTableShardingAlgorithm.class.getName());

        config.getShardingAlgorithms().put("picture_sharding_alg", shardingAlg);

        return config;
    }

    /**
     * 便捷创建 HikariCP 数据源
     */
    private DataSource createHikariDataSource(String poolName,
                                               String jdbcUrl,
                                               String username,
                                               String password) {
        com.zaxxer.hikari.HikariDataSource ds = new com.zaxxer.hikari.HikariDataSource();
        ds.setPoolName(poolName);
        ds.setJdbcUrl(jdbcUrl);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setMaximumPoolSize(20);
        ds.setMinimumIdle(5);
        ds.setIdleTimeout(300000);
        ds.setConnectionTimeout(20000);
        ds.setMaxLifetime(1200000);
        ds.setLeakDetectionThreshold(10000);
        return ds;
    }
}
