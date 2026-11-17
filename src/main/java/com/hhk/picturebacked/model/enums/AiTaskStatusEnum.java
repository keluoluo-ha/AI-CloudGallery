package com.hhk.picturebacked.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * AI 渲染任务状态
 */
@Getter
public enum AiTaskStatusEnum {

    /**
     * 排队中，已落库待消费
     */
    QUEUED("排队中", 0),

    /**
     * 处理中，已提交给第三方 AI
     */
    PROCESSING("处理中", 1),

    /**
     * 成功，产物已落库
     */
    SUCCESS("成功", 2),

    /**
     * 失败，可人工重试
     */
    FAILED("失败", 3);

    private final String text;

    private final int value;

    AiTaskStatusEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    public static AiTaskStatusEnum getEnumByValue(Integer value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (AiTaskStatusEnum statusEnum : AiTaskStatusEnum.values()) {
            if (statusEnum.value == value) {
                return statusEnum;
            }
        }
        return null;
    }

    /**
     * 是否为终态（成功或失败）
     */
    public boolean isFinal() {
        return this == SUCCESS || this == FAILED;
    }
}
