package com.hhk.picturebacked.manager.sharding;

import com.baomidou.mybatisplus.extension.toolkit.SqlRunner;
import com.hhk.picturebacked.model.entity.Space;
import com.hhk.picturebacked.model.enums.SpaceLevelEnum;
import com.hhk.picturebacked.model.enums.SpaceTypeEnum;
import com.hhk.picturebacked.service.SpaceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.driver.jdbc.core.connection.ShardingSphereConnection;
import org.apache.shardingsphere.infra.metadata.database.rule.ShardingSphereRuleMetaData;
import org.apache.shardingsphere.mode.manager.ContextManager;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration;
import org.apache.shardingsphere.sharding.rule.ShardingRule;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class DynamicShardingManager {

    @Resource
    private DataSource dataSource;

    @Resource
    private SpaceService spaceService;

    private static final String LOGIC_TABLE_NAME = "picture";

    private static final String DATABASE_NAME = "logic_db";

    @PostConstruct
    public void initialize() {
        log.info("初始化动态分表配置...");
        ensureFlagshipShardingTables();
        updateShardingTableNodes();
    }

    /**
     * 仅旗舰版团队空间使用分表 picture_{spaceId}
     */
    private Set<String> fetchAllPictureTableNames() {
        Set<Long> flagshipSpaceIds = spaceService.lambdaQuery()
                .eq(Space::getSpaceType, SpaceTypeEnum.TEAM.getValue())
                .eq(Space::getSpaceLevel, SpaceLevelEnum.FLAGSHIP.getValue())
                .list()
                .stream()
                .map(Space::getId)
                .collect(Collectors.toSet());

        Set<String> tableNames = flagshipSpaceIds.stream()
                .map(spaceId -> LOGIC_TABLE_NAME + "_" + spaceId)
                .collect(Collectors.toSet());
        tableNames.add(LOGIC_TABLE_NAME);
        return tableNames;
    }

    private void updateShardingTableNodes() {
        Set<String> tableNames = fetchAllPictureTableNames();
        String newActualDataNodes = tableNames.stream()
                .map(tableName -> "yu_picture." + tableName)
                .collect(Collectors.joining(","));
        log.info("动态分表 actual-data-nodes 配置: {}", newActualDataNodes);

        ContextManager contextManager = getContextManager();
        ShardingSphereRuleMetaData ruleMetaData = contextManager.getMetaDataContexts()
                .getMetaData()
                .getDatabases()
                .get(DATABASE_NAME)
                .getRuleMetaData();

        Optional<ShardingRule> shardingRule = ruleMetaData.findSingleRule(ShardingRule.class);
        if (shardingRule.isPresent()) {
            ShardingRuleConfiguration ruleConfig = (ShardingRuleConfiguration) shardingRule.get().getConfiguration();
            List<ShardingTableRuleConfiguration> updatedRules = ruleConfig.getTables()
                    .stream()
                    .map(oldTableRule -> {
                        if (LOGIC_TABLE_NAME.equals(oldTableRule.getLogicTable())) {
                            ShardingTableRuleConfiguration newTableRuleConfig =
                                    new ShardingTableRuleConfiguration(LOGIC_TABLE_NAME, newActualDataNodes);
                            newTableRuleConfig.setDatabaseShardingStrategy(oldTableRule.getDatabaseShardingStrategy());
                            newTableRuleConfig.setTableShardingStrategy(oldTableRule.getTableShardingStrategy());
                            newTableRuleConfig.setKeyGenerateStrategy(oldTableRule.getKeyGenerateStrategy());
                            newTableRuleConfig.setAuditStrategy(oldTableRule.getAuditStrategy());
                            return newTableRuleConfig;
                        }
                        return oldTableRule;
                    })
                    .collect(Collectors.toList());
            ruleConfig.setTables(updatedRules);
            contextManager.alterRuleConfiguration(DATABASE_NAME, Collections.singleton(ruleConfig));
            contextManager.reloadDatabase(DATABASE_NAME);
            log.info("动态分表规则更新成功！");
        } else {
            log.error("未找到 ShardingSphere 的分片规则配置，动态分表更新失败。");
        }
    }

    private ContextManager getContextManager() {
        try (ShardingSphereConnection connection = dataSource.getConnection().unwrap(ShardingSphereConnection.class)) {
            return connection.getContextManager();
        } catch (SQLException e) {
            throw new RuntimeException("获取 ShardingSphere ContextManager 失败", e);
        }
    }

    /**
     * 启动时补建缺失的旗舰版分表
     */
    private void ensureFlagshipShardingTables() {
        spaceService.lambdaQuery()
                .eq(Space::getSpaceType, SpaceTypeEnum.TEAM.getValue())
                .eq(Space::getSpaceLevel, SpaceLevelEnum.FLAGSHIP.getValue())
                .list()
                .forEach(this::createSpacePictureTableIfAbsent);
    }

    /**
     * 创建团队空间分表（仅旗舰版）
     */
    public void createSpacePictureTable(Space space) {
        if (space == null) {
            return;
        }
        if (SpaceTypeEnum.TEAM.getValue() != space.getSpaceType()
                || SpaceLevelEnum.FLAGSHIP.getValue() != space.getSpaceLevel()) {
            return;
        }
        createSpacePictureTableIfAbsent(space);
        updateShardingTableNodes();
    }

    private void createSpacePictureTableIfAbsent(Space space) {
        Long spaceId = space.getId();
        if (spaceId == null) {
            return;
        }
        String tableName = LOGIC_TABLE_NAME + "_" + spaceId;
        if (physicalTableExists(tableName)) {
            return;
        }
        String createTableSql = String.format("CREATE TABLE `%s` LIKE `picture`", tableName);
        try {
            SqlRunner.db().update(createTableSql);
            log.info("创建图片分表成功: {}", tableName);
        } catch (Exception e) {
            log.error("创建图片空间分表失败，空间 id = {}", spaceId, e);
        }
    }

    private boolean physicalTableExists(String tableName) {
        String sql = "SHOW TABLES LIKE {0}";
        List<Object> result = SqlRunner.db().selectObjs(sql, tableName);
        return result != null && !result.isEmpty();
    }
}
