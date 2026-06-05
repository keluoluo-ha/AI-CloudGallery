package com.hhk.picturebacked.manager.sharding;

import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.Collection;
import java.util.Properties;

public class PictureShardingAlgorithm implements StandardShardingAlgorithm<Comparable<?>> {

    @Override
    public String doSharding(Collection<String> availableTargetNames, PreciseShardingValue<Comparable<?>> preciseShardingValue) {
        Long spaceId = parseSpaceId(preciseShardingValue.getValue());
        String logicTableName = preciseShardingValue.getLogicTableName();
        // 公共图库（spaceId 为空或 0）走主表
        if (spaceId == null || spaceId == 0L) {
            return logicTableName;
        }
        String realTableName = logicTableName + "_" + spaceId;
        if (availableTargetNames.contains(realTableName)) {
            return realTableName;
        }
        return logicTableName;
    }

    @Override
    public Collection<String> doSharding(Collection<String> collection, RangeShardingValue<Comparable<?>> rangeShardingValue) {
        return collection;
    }

    @Override
    public Properties getProps() {
        return new Properties();
    }

    @Override
    public void init(Properties properties) {
    }

    /**
     * ShardingSphere 传入的分片值可能是 Integer / Long，需要安全转换
     */
    private Long parseSpaceId(Comparable<?> value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.valueOf(value.toString());
    }
}
