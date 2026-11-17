package com.hhk.picturebacked.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * AI 渲染任务类型
 */
@Getter
public enum AiTaskTypeEnum {

    /**
     * 智能抠图，输出透明底
     */
    REMOVE_BG("智能抠图", "REMOVE_BG"),

    /**
     * 白底合规图，输出纯白底
     */
    WHITE_BG("白底合规图", "WHITE_BG"),

    /**
     * AI 场景图，输出指定场景背景
     */
    SCENE("AI 场景图", "SCENE"),

    /**
     * 规格适配，纯尺寸/比例处理，不调 AI
     */
    RESIZE("规格适配", "RESIZE");

    private final String text;

    private final String value;

    AiTaskTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static AiTaskTypeEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (AiTaskTypeEnum typeEnum : AiTaskTypeEnum.values()) {
            if (typeEnum.value.equals(value)) {
                return typeEnum;
            }
        }
        return null;
    }

    /**
     * 是否需要调用第三方 AI（RESIZE 只做本地尺寸处理）
     */
    public boolean needAiCall() {
        return this != RESIZE;
    }
}
