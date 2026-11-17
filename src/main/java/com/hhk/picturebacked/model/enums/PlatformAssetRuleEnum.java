package com.hhk.picturebacked.model.enums;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 平台素材齐备规则
 * <p>
 * 定义每个平台需要哪些素材类型才算「齐备」，用于计算素材覆盖率与缺口。
 * 本期写死，不做运营后台可配置。
 * </p>
 * <p>
 * 当前所有平台都要求至少有一张主图（MAIN）；
 * TikTok Shop 因为有竖版封面要求，额外要求一张竖版封面素材。
 * </p>
 */
@Getter
public class PlatformAssetRuleEnum {

    /**
     * 亚马逊：主图
     */
    public static final PlatformAssetRuleEnum AMAZON = new PlatformAssetRuleEnum(
            "AMAZON", "亚马逊", Arrays.asList(AssetTypeEnum.MAIN));

    /**
     * 虾皮：主图
     */
    public static final PlatformAssetRuleEnum SHOPEE = new PlatformAssetRuleEnum(
            "SHOPEE", "虾皮", Arrays.asList(AssetTypeEnum.MAIN));

    /**
     * TikTok Shop：主图 + 竖版封面（封面归入 MAIN 的竖版，用 SCENE 近似不合适，这里额外要求一张场景/封面图）
     */
    public static final PlatformAssetRuleEnum TIKTOK = new PlatformAssetRuleEnum(
            "TIKTOK", "TikTok Shop", Arrays.asList(AssetTypeEnum.MAIN, AssetTypeEnum.SCENE));

    /**
     * Temu：主图
     */
    public static final PlatformAssetRuleEnum TEMU = new PlatformAssetRuleEnum(
            "TEMU", "Temu", Arrays.asList(AssetTypeEnum.MAIN));

    /**
     * 平台枚举值
     */
    private final String platform;

    /**
     * 平台中文名
     */
    private final String platformName;

    /**
     * 该平台要求具备的素材类型
     */
    private final List<AssetTypeEnum> requiredAssetTypes;

    private PlatformAssetRuleEnum(String platform, String platformName, List<AssetTypeEnum> requiredAssetTypes) {
        this.platform = platform;
        this.platformName = platformName;
        this.requiredAssetTypes = requiredAssetTypes;
    }

    /**
     * 全部规则（不含 COMMON，通用素材不参与齐备率计算）
     */
    public static List<PlatformAssetRuleEnum> allRules() {
        return new ArrayList<>(Arrays.asList(AMAZON, SHOPEE, TIKTOK, TEMU));
    }

    /**
     * 按平台值获取规则，找不到返回 null
     */
    public static PlatformAssetRuleEnum getRuleByPlatform(String platform) {
        if (platform == null) {
            return null;
        }
        for (PlatformAssetRuleEnum rule : allRules()) {
            if (rule.getPlatform().equals(platform)) {
                return rule;
            }
        }
        return null;
    }
}
