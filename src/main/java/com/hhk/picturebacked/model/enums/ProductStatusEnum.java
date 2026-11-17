package com.hhk.picturebacked.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 商品状态
 */
@Getter
public enum ProductStatusEnum {

    DRAFT("草稿", 0),
    ON_SALE("在售", 1),
    OFF_SALE("停售", 2);

    private final String text;

    private final int value;

    ProductStatusEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     */
    public static ProductStatusEnum getEnumByValue(Integer value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (ProductStatusEnum productStatusEnum : ProductStatusEnum.values()) {
            if (productStatusEnum.value == value) {
                return productStatusEnum;
            }
        }
        return null;
    }
}
