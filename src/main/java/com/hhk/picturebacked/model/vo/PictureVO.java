package com.hhk.picturebacked.model.vo;

import cn.hutool.json.JSONUtil;
import com.hhk.picturebacked.model.entity.Picture;
import com.hhk.picturebacked.utils.CosUrlUtils;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class PictureVO implements Serializable {
  
    /**  
     * id  
     */  
    private Long id;  
  
    /**  
     * 图片 url  
     */  
    private String url;  
  
    /**  
     * 图片名称  
     */  
    private String name;  
  
    /**  
     * 简介  
     */  
    private String introduction;  
  
    /**  
     * 标签  
     */  
    private List<String> tags;
  
    /**  
     * 分类  
     */  
    private String category;  
  
    /**  
     * 文件体积  
     */  
    private Long picSize;  
  
    /**  
     * 图片宽度  
     */  
    private Integer picWidth;  
  
    /**  
     * 图片高度  
     */  
    private Integer picHeight;  
  
    /**  
     * 图片比例  
     */  
    private Double picScale;  
  
    /**  
     * 图片格式  
     */  
    private String picFormat;  
  
    /**  
     * 用户 id  
     */  
    private Long userId;  
  
    /**  
     * 创建时间  
     */  
    private Date createTime;
  
    /**  
     * 编辑时间  
     */  
    private Date editTime;  
  
    /**  
     * 更新时间  
     */  
    private Date updateTime;  
  
    /**  
     * 创建用户信息  
     */  
    private UserVO user;

    /**
     * 缩略图 url
     */
    private String thumbnailUrl;
    /**
     * 空间 id
     */
    private Long spaceId;
    /**
     * 图片主色调
     */
    private String picColor;
    /**
     * 浏览量
     */
    private Long viewCount;
    /**
     * 归属商品 id（SKU）
     */
    private Long productId;
    /**
     * 素材类型：MAIN / DETAIL / SCENE / WHITE / DETAIL_LONG
     */
    private String assetType;
    /**
     * 目标平台：AMAZON / SHOPEE / TIKTOK / TEMU / COMMON
     */
    private String platform;
    /**
     * 来源原图 id
     */
    private Long sourceId;
    /**
     * 语言：zh / en
     */
    private String lang;
    /**
     * 审核状态：0-待审核 1-通过 2-驳回
     * <p>
     * 电商场景下 AI 产物默认落在待审核，必须经人审才能上架，
     * 所以前端需要能直接读到这个状态做卡点。
     * </p>
     */
    private Integer reviewStatus;
    /**
     * 审核备注
     */
    private String reviewMessage;
    /**
     * 审核人 id
     */
    private Long reviewerId;
    /**
     * 审核时间
     */
    private Date reviewTime;
    /**
     * 权限列表
     */
    private List<String> permissionList = new ArrayList<>();

    private static final long serialVersionUID = 1L;  
  
    /**  
     * 封装类转对象  
     */  
    public static Picture voToObj(PictureVO pictureVO) {
        if (pictureVO == null) {  
            return null;  
        }  
        Picture picture = new Picture();  
        BeanUtils.copyProperties(pictureVO, picture);
        // 类型不同，需要转换  
        picture.setTags(JSONUtil.toJsonStr(pictureVO.getTags()));
        return picture;  
    }  
  
    /**  
     * 对象转封装类  
     */  
    public static PictureVO objToVo(Picture picture) {  
        if (picture == null) {  
            return null;  
        }  
        PictureVO pictureVO = new PictureVO();  
        BeanUtils.copyProperties(picture, pictureVO);  
        // 类型不同，需要转换  
        pictureVO.setTags(JSONUtil.toList(picture.getTags(), String.class));
        pictureVO.setUrl(CosUrlUtils.normalizeAccessUrl(pictureVO.getUrl()));
        pictureVO.setThumbnailUrl(CosUrlUtils.normalizeAccessUrl(pictureVO.getThumbnailUrl()));
        return pictureVO;  
    }  
}
