-- ============================================================
-- 跨境电商素材中台 · T1 数据库最小改动
-- 目标：新建 product 商品表 + picture 表加 5 个电商字段
-- 说明：不改动已有字段，老数据全部兼容（新增字段可空）
-- ============================================================

-- 1. 商品表（一个 Space = 一个店铺，product = 店铺下的一个 SKU）
-- 注意：本项目列名统一用小驼峰（application.yml 里 map-underscore-to-camel-case: false，
--      MyBatis-Plus 不会自动转换，列名必须与实体字段完全一致）。
CREATE TABLE IF NOT EXISTS `product` (
    `id`           BIGINT       NOT NULL                COMMENT '商品 id',
    `spaceId`      BIGINT       NOT NULL DEFAULT 0      COMMENT '所属空间 id（店铺）',
    `sku`          VARCHAR(64)  NOT NULL DEFAULT ''     COMMENT 'SKU 编码，店铺内唯一',
    `title`        VARCHAR(255) NOT NULL DEFAULT ''     COMMENT '商品名称',
    `coverId`      BIGINT                DEFAULT NULL    COMMENT '封面图片 id',
    `status`       TINYINT      NOT NULL DEFAULT 0      COMMENT '状态：0-草稿 1-在售 2-停售',
    `userId`       BIGINT                DEFAULT NULL    COMMENT '创建用户 id',
    `createTime`   DATETIME              DEFAULT NULL    COMMENT '创建时间',
    `editTime`     DATETIME              DEFAULT NULL    COMMENT '编辑时间',
    `updateTime`   DATETIME              DEFAULT NULL    COMMENT '更新时间',
    `isDelete`     TINYINT      NOT NULL DEFAULT 0      COMMENT '是否删除：0-否 1-是',
    PRIMARY KEY (`id`),
    KEY `idx_space_sku` (`spaceId`, `sku`, `isDelete`),
    KEY `idx_space_id` (`spaceId`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '商品（SKU）';

-- 2. picture 表新增电商字段
-- 注意：
--      1) picture 是按 spaceId 动态分表的（picture / picture_<spaceId>），
--         物理分表由 DynamicShardingManager 用 CREATE TABLE LIKE 自动创建，
--         所以只要改主表即可，后续新分表会自动继承这些列。
--      2) 本项目的列名统一用小驼峰（与 application.yml 里
--         map-underscore-to-camel-case: false 的设置保持一致）。
--      3) 公共图库走主表 picture，同样包含这些列。
ALTER TABLE `picture`
    ADD COLUMN `productId` BIGINT                DEFAULT NULL COMMENT '归属商品 id（SKU），为空表示未绑定' AFTER `spaceId`,
    ADD COLUMN `assetType` VARCHAR(16)           DEFAULT NULL COMMENT '素材类型：MAIN-主图 DETAIL-细节 SCENE-场景 WHITE-白底 DETAIL_LONG-长图' AFTER `productId`,
    ADD COLUMN `platform`   VARCHAR(16)           DEFAULT NULL COMMENT '目标平台：AMAZON SHOPEE TIKTOK TEMU COMMON' AFTER `assetType`,
    ADD COLUMN `sourceId`   BIGINT                DEFAULT NULL COMMENT '来源原图 id，衍生图指向原图' AFTER `platform`,
    ADD COLUMN `lang`       VARCHAR(16)           DEFAULT NULL COMMENT '语言：zh en' AFTER `sourceId`,
    ADD COLUMN `view_count` BIGINT                DEFAULT NULL COMMENT '浏览量（定时任务批量刷库）' AFTER `picColor`;

-- 3. 素材类型与平台取值参考（枚举值硬编码在代码中，本期不做配置表）
--    asset_type: MAIN / DETAIL / SCENE / WHITE / DETAIL_LONG
--    platform:   AMAZON / SHOPEE / TIKTOK / TEMU / COMMON
