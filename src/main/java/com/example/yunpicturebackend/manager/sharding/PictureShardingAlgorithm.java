package com.example.yunpicturebackend.manager.sharding;

import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.Collection;
import java.util.Collections;
import java.util.Properties;

/**
 * 图片分表算法
 */
public class PictureShardingAlgorithm implements StandardShardingAlgorithm<Long> {

    @Override
    public String doSharding(Collection<String> availableTargetNames, PreciseShardingValue<Long> preciseShardingValue) {
        Long spaceId = preciseShardingValue.getValue();
        String logicTableName = preciseShardingValue.getLogicTableName();
        
        // spaceId 为 null 表示查询所有图片
        if (spaceId == null) {
            // 如果有可用表，返回第一个；否则返回逻辑表名
            if (!availableTargetNames.isEmpty()) {
                return availableTargetNames.iterator().next();
            } else {
                return logicTableName;
            }
        }
        
        // 根据 spaceId 动态生成分表名
        String realTableName = "picture_" + spaceId;
        if (availableTargetNames.contains(realTableName)) {
            return realTableName;
        } else {
            // 如果特定的分表不存在，使用默认的分表
            String defaultTableName = "picture_0";
            if (availableTargetNames.contains(defaultTableName)) {
                return defaultTableName;
            }
            // 如果默认表也不存在，则返回逻辑表名
            return logicTableName;
        }
    }

    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames, RangeShardingValue<Long> rangeShardingValue) {
        // 对于范围查询，返回所有可用的表
        // 如果没有可用的表，返回空集合而不是抛出异常
        return availableTargetNames.isEmpty() ? Collections.emptySet() : availableTargetNames;
    }

    @Override
    public Properties getProps() {
        return null;
    }

    @Override
    public void init(Properties properties) {

    }
}