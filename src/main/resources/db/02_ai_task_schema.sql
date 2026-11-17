-- ============================================================
-- 跨境电商素材中台 · T5 AI 渲染任务表
-- 目标：AI 产图异步化，提交即返回，状态可查
-- 说明：薄表设计，不做工作流引擎，失败人工重试
-- ============================================================

-- 注意：本项目列名统一用小驼峰（application.yml 里 map-underscore-to-camel-case: false）。
CREATE TABLE IF NOT EXISTS `ai_task` (
    `id`                 BIGINT       NOT NULL                COMMENT '任务 id',
    `spaceId`            BIGINT       NOT NULL DEFAULT 0      COMMENT '所属空间 id（店铺）',
    `productId`          BIGINT                DEFAULT NULL    COMMENT '归属商品 id（SKU）',
    `sourcePictureId`   BIGINT       NOT NULL                COMMENT '来源原图 id',
    `type`               VARCHAR(16)  NOT NULL                COMMENT '任务类型：REMOVE_BG-抠图 WHITE_BG-白底图 SCENE-场景图 RESIZE-规格适配',
    `platform`           VARCHAR(16)           DEFAULT NULL    COMMENT '目标平台：AMAZON SHOPEE TIKTOK TEMU COMMON',
    `assetType`          VARCHAR(16)           DEFAULT NULL    COMMENT '产出的素材类型',
    `template`           VARCHAR(32)           DEFAULT NULL    COMMENT '场景模板标识',
    `status`             TINYINT      NOT NULL DEFAULT 0      COMMENT '状态：0-排队中 1-处理中 2-成功 3-失败',
    `aiTaskId`           VARCHAR(64)           DEFAULT NULL    COMMENT '第三方 AI 任务 id',
    `resultPictureId`    BIGINT                DEFAULT NULL    COMMENT '成功后生成的图片 id',
    `errorMessage`       VARCHAR(512)          DEFAULT NULL    COMMENT '失败原因',
    `paramJson`          VARCHAR(1024)         DEFAULT NULL    COMMENT '任务参数快照',
    `requestKey`         VARCHAR(128)          DEFAULT NULL    COMMENT '幂等键：源图+类型+平台+模板',
    `userId`             BIGINT                DEFAULT NULL    COMMENT '提交用户 id',
    `createTime`         DATETIME              DEFAULT NULL    COMMENT '创建时间',
    `updateTime`         DATETIME              DEFAULT NULL    COMMENT '更新时间',
    `finishTime`         DATETIME              DEFAULT NULL    COMMENT '完成时间',
    `isDelete`           TINYINT      NOT NULL DEFAULT 0      COMMENT '是否删除：0-否 1-是',
    PRIMARY KEY (`id`),
    KEY `idx_space_status` (`spaceId`, `status`),
    KEY `idx_request_key` (`requestKey`),
    KEY `idx_source` (`sourcePictureId`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT 'AI 渲染任务';
