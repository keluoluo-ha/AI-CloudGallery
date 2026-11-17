package com.hhk.picturebacked.model.dto.ai;

import lombok.Data;

import java.io.Serializable;

/**
 * AI 渲染任务提交请求
 */
@Data
public class AiRenderCreateRequest implements Serializable {

    /**
     * 来源原图 id
     */
    private Long sourcePictureId;

    /**
     * 任务类型：REMOVE_BG / WHITE_BG / SCENE / RESIZE
     */
    private String type;

    /**
     * 目标平台：AMAZON / SHOPEE / TIKTOK / TEMU / COMMON
     */
    private String platform;

    /**
     * 场景模板标识，type = SCENE 时必填
     */
    private String template;

    /**
     * 产出的素材类型：MAIN / DETAIL / SCENE / WHITE / DETAIL_LONG
     * 为空时按任务类型自动推断
     */
    private String assetType;

    /**
     * 语言：zh / en
     */
    private String lang;

    private static final long serialVersionUID = 1L;
}
