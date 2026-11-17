package com.hhk.picturebacked.model.vo;

import com.hhk.picturebacked.model.entity.Picture;
import lombok.Data;

import java.io.Serializable;

/**
 * 单张图片的规格校验结果
 */
@Data
public class PictureSpecCheckVO implements Serializable {

    /**
     * 图片 id
     */
    private Long pictureId;

    /**
     * 图片名称
     */
    private String pictureName;

    /**
     * 校验结果
     */
    private SpecCheckResultVO result;

    /**
     * 构造
     */
    public static PictureSpecCheckVO objToVo(Picture picture, SpecCheckResultVO result) {
        PictureSpecCheckVO vo = new PictureSpecCheckVO();
        vo.setPictureId(picture == null ? null : picture.getId());
        vo.setPictureName(picture == null ? null : picture.getName());
        vo.setResult(result);
        return vo;
    }

    private static final long serialVersionUID = 1L;
}
