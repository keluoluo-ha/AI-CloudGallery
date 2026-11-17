package com.hhk.picturebacked.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 素材类型
 * 对应跨境电商图的不同用途
 */
@Getter
public enum AssetTypeEnum {

    MAIN("主图", "MAIN"),
    DETAIL("细节图", "DETAIL"),
    SCENE("场景图", "SCENE"),
    WHITE("白底图", "WHITE"),
    DETAIL_LONG("详情长图", "DETAIL_LONG");

    private final String text;

    private final String value;

    AssetTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     */
    public static AssetTypeEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (AssetTypeEnum assetTypeEnum : AssetTypeEnum.values()) {
            if (assetTypeEnum.value.equals(value)) {
                return assetTypeEnum;
            }
        }
        return null;
    }
}
