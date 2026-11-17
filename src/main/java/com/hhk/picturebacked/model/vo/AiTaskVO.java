package com.hhk.picturebacked.model.vo;

import com.hhk.picturebacked.model.entity.AiTask;
import com.hhk.picturebacked.model.enums.AiTaskStatusEnum;
import com.hhk.picturebacked.model.enums.AiTaskTypeEnum;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * AI 渲染任务视图对象
 */
@Data
public class AiTaskVO implements Serializable {

    /**
     * 任务 id
     */
    private Long id;

    /**
     * 所属空间 id
     */
    private Long spaceId;

    /**
     * 归属商品 id
     */
    private Long productId;

    /**
     * 来源原图 id
     */
    private Long sourcePictureId;

    /**
     * 来源原图 url
     */
    private String sourcePictureUrl;

    /**
     * 任务类型值
     */
    private String type;

    /**
     * 任务类型文案
     */
    private String typeText;

    /**
     * 目标平台
     */
    private String platform;

    /**
     * 场景模板
     */
    private String template;

    /**
     * 产出的素材类型
     */
    private String assetType;

    /**
     * 状态值
     */
    private Integer status;

    /**
     * 状态文案
     */
    private String statusText;

    /**
     * 成功后生成的图片 id
     */
    private Long resultPictureId;

    /**
     * 产物图片 url
     */
    private String resultPictureUrl;

    /**
     * 失败原因
     */
    private String errorMessage;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 完成时间
     */
    private Date finishTime;

    /**
     * 实体转 VO
     */
    public static AiTaskVO objToVo(AiTask task) {
        if (task == null) {
            return null;
        }
        AiTaskVO vo = new AiTaskVO();
        vo.setId(task.getId());
        vo.setSpaceId(task.getSpaceId());
        vo.setProductId(task.getProductId());
        vo.setSourcePictureId(task.getSourcePictureId());
        vo.setType(task.getType());
        AiTaskTypeEnum typeEnum = AiTaskTypeEnum.getEnumByValue(task.getType());
        vo.setTypeText(typeEnum == null ? task.getType() : typeEnum.getText());
        vo.setPlatform(task.getPlatform());
        vo.setTemplate(task.getTemplate());
        vo.setAssetType(task.getAssetType());
        vo.setStatus(task.getStatus());
        AiTaskStatusEnum statusEnum = AiTaskStatusEnum.getEnumByValue(task.getStatus());
        vo.setStatusText(statusEnum == null ? "未知" : statusEnum.getText());
        vo.setResultPictureId(task.getResultPictureId());
        vo.setErrorMessage(task.getErrorMessage());
        vo.setCreateTime(task.getCreateTime());
        vo.setFinishTime(task.getFinishTime());
        return vo;
    }

    private static final long serialVersionUID = 1L;
}
