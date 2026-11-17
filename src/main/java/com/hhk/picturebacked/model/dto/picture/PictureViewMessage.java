package com.hhk.picturebacked.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 浏览量消息体：只放两个字段，尽量小，减少 MQ 传输开销
 */
@Data
public class PictureViewMessage implements Serializable {

    /**
     * 图片 id
     */
    private Long pictureId;

    /**
     * 上报用户 id（未登录时为 0 或负数 ip 哈希）
     */
    private Long userId;

    private static final long serialVersionUID = 1L;
}
