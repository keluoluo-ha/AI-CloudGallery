package com.hhk.picturebacked.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 平台图片规格
 * <p>
 * 本期写死，不做运营后台可配置。规则来源各平台公开的图片规范：
 * Amazon 主图要求纯白底、1:1、85% 以上商品占比；
 * Shopee 首图 1:1 且不允许拼图；
 * TikTok Shop 方图 + 竖版封面，且竖版需预留标题安全区（顶部约 12%）；
 * Temu 白底审核最严，文字占比需极小。
 * </p>
 */
@Getter
public enum PlatformSpecEnum {

    AMAZON("亚马逊", "AMAZON",
            // 主图尺寸
            2000, 2000,
            // 详情 / 附图尺寸
            2000, 2000,
            // 单图最大体积 MB
            10,
            // 图片格式
            "JPG",
            // 是否要求纯白底
            true,
            // 商品占框比例（%）
            85,
            // 是否禁止拼图
            true,
            // 竖版封面尺寸（宽、高），无则为 0
            0, 0,
            // 标题安全区（占高度百分比，0 表示无要求）
            0,
            // 人工审核强度：0-普通 1-严格
            1,
            "主图必须纯白底(255,255,255)、1:1 正方形、不得含文字水印 Logo，商品占画面 85% 以上"),

    SHOPEE("虾皮", "SHOPEE",
            800, 800,
            800, 800,
            2,
            "JPG",
            false,
            80,
            true,
            0, 0,
            0,
            0,
            "首图需 1:1 方形、建议 800x800 以上，不得为拼图，不得带促销文字"),

    TIKTOK("TikTok Shop", "TIKTOK",
            1080, 1080,
            1080, 1080,
            10,
            "JPG",
            false,
            75,
            false,
            720, 1280,
            12,
            0,
            "主图为 1:1 方图；竖版封面 720x1280，顶部约 12% 为标题安全区，商品主体需避开"),

    TEMU("Temu", "TEMU",
            800, 800,
            1200, 1200,
            10,
            "JPG",
            true,
            85,
            true,
            0, 0,
            0,
            1,
            "白底审核最严：必须纯白底、1:1、不得含文字/促销标识/拼接，商品居中且占比充足");

    /**
     * 平台中文名
     */
    private final String text;

    /**
     * 平台枚举值
     */
    private final String value;

    /**
     * 主图宽度
     */
    private final int mainWidth;

    /**
     * 主图高度
     */
    private final int mainHeight;

    /**
     * 详情/附图宽度
     */
    private final int detailWidth;

    /**
     * 详情/附图高度
     */
    private final int detailHeight;

    /**
     * 单图最大体积（MB）
     */
    private final int maxSizeMb;

    /**
     * 图片格式
     */
    private final String format;

    /**
     * 是否要求纯白底
     */
    private final boolean whiteBackgroundRequired;

    /**
     * 商品占框比例（%）
     */
    private final int productRatio;

    /**
     * 是否禁止拼图
     */
    private final boolean forbidCollage;

    /**
     * 竖版封面宽度，0 表示无竖版要求
     */
    private final int coverWidth;

    /**
     * 竖版封面高度，0 表示无竖版要求
     */
    private final int coverHeight;

    /**
     * 标题安全区（占高度百分比），0 表示无要求
     */
    private final int titleSafeAreaPercent;

    /**
     * 人工审核强度：0-普通 1-严格
     */
    private final int reviewStrictness;

    /**
     * 平台规则说明
     */
    private final String rule;

    PlatformSpecEnum(String text, String value,
                     int mainWidth, int mainHeight,
                     int detailWidth, int detailHeight,
                     int maxSizeMb, String format,
                     boolean whiteBackgroundRequired, int productRatio, boolean forbidCollage,
                     int coverWidth, int coverHeight, int titleSafeAreaPercent,
                     int reviewStrictness, String rule) {
        this.text = text;
        this.value = value;
        this.mainWidth = mainWidth;
        this.mainHeight = mainHeight;
        this.detailWidth = detailWidth;
        this.detailHeight = detailHeight;
        this.maxSizeMb = maxSizeMb;
        this.format = format;
        this.whiteBackgroundRequired = whiteBackgroundRequired;
        this.productRatio = productRatio;
        this.forbidCollage = forbidCollage;
        this.coverWidth = coverWidth;
        this.coverHeight = coverHeight;
        this.titleSafeAreaPercent = titleSafeAreaPercent;
        this.reviewStrictness = reviewStrictness;
        this.rule = rule;
    }

    /**
     * 根据平台枚举值获取规格
     */
    public static PlatformSpecEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (PlatformSpecEnum specEnum : PlatformSpecEnum.values()) {
            if (specEnum.value.equals(value)) {
                return specEnum;
            }
        }
        return null;
    }

    /**
     * 是否有竖版封面要求
     */
    public boolean hasCover() {
        return coverWidth > 0 && coverHeight > 0;
    }

    /**
     * 是否需要强制人工审核
     */
    public boolean needStrictReview() {
        return reviewStrictness == 1;
    }
}
