package com.hhk.picturebacked.model.vo;

import com.hhk.picturebacked.model.enums.PlatformSpecEnum;
import lombok.Data;

import java.io.Serializable;

/**
 * 平台图片规格
 * <p>
 * 前端据此做裁切画布尺寸提示，后端 AI 渲染据此生成目标尺寸。
 * </p>
 */
@Data
public class PlatformSpecVO implements Serializable {

    /**
     * 平台枚举值：AMAZON / SHOPEE / TIKTOK / TEMU
     */
    private String platform;

    /**
     * 平台中文名
     */
    private String platformName;

    /**
     * 主图宽度
     */
    private Integer mainWidth;

    /**
     * 主图高度
     */
    private Integer mainHeight;

    /**
     * 详情/附图宽度
     */
    private Integer detailWidth;

    /**
     * 详情/附图高度
     */
    private Integer detailHeight;

    /**
     * 单图最大体积（MB）
     */
    private Integer maxSizeMb;

    /**
     * 图片格式
     */
    private String format;

    /**
     * 是否要求纯白底
     */
    private Boolean whiteBackgroundRequired;

    /**
     * 商品占框比例（%）
     */
    private Integer productRatio;

    /**
     * 是否禁止拼图
     */
    private Boolean forbidCollage;

    /**
     * 竖版封面宽度，0 表示无竖版要求
     */
    private Integer coverWidth;

    /**
     * 竖版封面高度，0 表示无竖版要求
     */
    private Integer coverHeight;

    /**
     * 是否有竖版封面要求
     */
    private Boolean hasCover;

    /**
     * 标题安全区（占高度百分比），0 表示无要求
     */
    private Integer titleSafeAreaPercent;

    /**
     * 是否需要强制人工审核
     */
    private Boolean needStrictReview;

    /**
     * 平台规则说明
     */
    private String rule;

    /**
     * 枚举转 VO
     */
    public static PlatformSpecVO objToVo(PlatformSpecEnum spec) {
        if (spec == null) {
            return null;
        }
        PlatformSpecVO vo = new PlatformSpecVO();
        vo.setPlatform(spec.getValue());
        vo.setPlatformName(spec.getText());
        vo.setMainWidth(spec.getMainWidth());
        vo.setMainHeight(spec.getMainHeight());
        vo.setDetailWidth(spec.getDetailWidth());
        vo.setDetailHeight(spec.getDetailHeight());
        vo.setMaxSizeMb(spec.getMaxSizeMb());
        vo.setFormat(spec.getFormat());
        vo.setWhiteBackgroundRequired(spec.isWhiteBackgroundRequired());
        vo.setProductRatio(spec.getProductRatio());
        vo.setForbidCollage(spec.isForbidCollage());
        vo.setCoverWidth(spec.getCoverWidth());
        vo.setCoverHeight(spec.getCoverHeight());
        vo.setHasCover(spec.hasCover());
        vo.setTitleSafeAreaPercent(spec.getTitleSafeAreaPercent());
        vo.setNeedStrictReview(spec.needStrictReview());
        vo.setRule(spec.getRule());
        return vo;
    }

    private static final long serialVersionUID = 1L;
}
