package com.hhk.picturebacked.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 电商平台
 * 本期写死，不做运营后台可配置
 */
@Getter
public enum PlatformEnum {

    AMAZON("亚马逊", "AMAZON"),
    SHOPEE("虾皮", "SHOPEE"),
    TIKTOK("TikTok Shop", "TIKTOK"),
    TEMU("Temu", "TEMU"),
    COMMON("通用", "COMMON");

    private final String text;

    private final String value;

    PlatformEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     */
    public static PlatformEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (PlatformEnum platformEnum : PlatformEnum.values()) {
            if (platformEnum.value.equals(value)) {
                return platformEnum;
            }
        }
        return null;
    }
}
