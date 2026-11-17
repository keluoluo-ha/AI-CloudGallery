package com.hhk.picturebacked.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 刷库用的增量条目：图片 id + 增量数
 */
@Data
public class PictureViewCountItem implements Serializable {

    /**
     * 图片 id
     */
    private Long pictureId;

    /**
     * 增量浏览量
     */
    private Long count;

    public PictureViewCountItem() {
    }

    public PictureViewCountItem(Long pictureId, Long count) {
        this.pictureId = pictureId;
        this.count = count;
    }

    private static final long serialVersionUID = 1L;
}
