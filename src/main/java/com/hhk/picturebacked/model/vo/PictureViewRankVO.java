package com.hhk.picturebacked.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 周榜条目：只返回图片 id + 浏览量
 */
@Data
public class PictureViewRankVO implements Serializable {

    /**
     * 图片 id
     */
    private Long pictureId;

    /**
     * 浏览量
     */
    private Long viewCount;

    private static final long serialVersionUID = 1L;
}
