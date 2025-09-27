package com.example.yunpicturebackend.manager.sharding;

import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.Collection;
import java.util.Collections;
import java.util.Properties;

/**
 * 图片分表算法
 */
@Slf4j
public class PictureShardingAlgorithm implements StandardShardingAlgorithm<Long> {

    @Override
    public String doSharding(Collection<String> availableTargetNames, PreciseShardingValue<Long> preciseShardingValue) {
        Long spaceId = preciseShardingValue.getValue();
        
        log.info("分片算法执行 - spaceId: {}, 可用表: {}", spaceId, availableTargetNames);

        // 如果 spaceId 为 null 或 -1（公共空间），使用 picture_0
        if (spaceId == null || spaceId == -1L) {
            log.info("spaceId为null或-1（公共空间），使用picture_0");
            return "picture_0";
        }

        // 根据 spaceId 计算分表，使用3个分表（0-2）
        String tableName = "picture_" + (Math.abs(spaceId) % 3);
        
        log.info("计算得到表名: {}", tableName);

        // 确保返回的表名在可用目标中
        if (availableTargetNames.contains(tableName)) {
            log.info("表名{}在可用目标中，返回该表", tableName);
            return tableName;
        }

        // 如果特定表不存在，返回默认表
        log.warn("表名{}不在可用目标中，使用默认表picture_0", tableName);
        return "picture_0";
    }

    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames, RangeShardingValue<Long> rangeShardingValue) {
        log.info("范围查询分片 - 可用表: {}, 范围值: {}", availableTargetNames, rangeShardingValue);
        
        // 对于范围查询，始终返回单个表以避免插入操作路由到多个节点
        // 这确保插入操作只会路由到一个数据节点
        log.info("范围查询返回默认表picture_0以避免多节点路由");
        return Collections.singletonList("picture_0");
    }

    @Override
    public Properties getProps() {
        return new Properties();
    }

    @Override
    public void init(Properties properties) {
        // 初始化逻辑
    }
}
