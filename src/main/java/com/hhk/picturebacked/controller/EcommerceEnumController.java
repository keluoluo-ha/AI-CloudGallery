package com.hhk.picturebacked.controller;

import com.hhk.picturebacked.commom.BaseResponse;
import com.hhk.picturebacked.commom.ResultUtils;
import com.hhk.picturebacked.model.enums.AssetTypeEnum;
import com.hhk.picturebacked.model.enums.PlatformEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 电商素材枚举：素材类型、目标平台
 * 供前端下拉选择使用，本期写死不做配置后台
 */
@RestController
@RequestMapping("/ecommerce")
@Slf4j
public class EcommerceEnumController {

    /**
     * 素材类型列表
     */
    @GetMapping("/asset_type/list")
    public BaseResponse<List<AssetTypeOption>> listAssetType() {
        List<AssetTypeOption> list = Arrays.stream(AssetTypeEnum.values())
                .map(e -> new AssetTypeOption(e.getValue(), e.getText()))
                .collect(Collectors.toList());
        return ResultUtils.success(list);
    }

    /**
     * 目标平台列表
     */
    @GetMapping("/platform/list")
    public BaseResponse<List<PlatformOption>> listPlatform() {
        List<PlatformOption> list = Arrays.stream(PlatformEnum.values())
                .map(e -> new PlatformOption(e.getValue(), e.getText()))
                .collect(Collectors.toList());
        return ResultUtils.success(list);
    }

    /**
     * 素材类型选项
     */
    public static class AssetTypeOption {
        private String value;
        private String text;

        public AssetTypeOption(String value, String text) {
            this.value = value;
            this.text = text;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    /**
     * 平台选项
     */
    public static class PlatformOption {
        private String value;
        private String text;

        public PlatformOption(String value, String text) {
            this.value = value;
            this.text = text;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}
