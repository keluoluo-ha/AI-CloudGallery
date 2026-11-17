package com.hhk.picturebacked.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hhk.picturebacked.api.Aliyunimage.AliYunImageEditApi;
import com.hhk.picturebacked.config.RabbitMQConfig;
import com.hhk.picturebacked.exception.BusinessException;
import com.hhk.picturebacked.exception.ErrorCode;
import com.hhk.picturebacked.exception.ThrowUtils;
import com.hhk.picturebacked.mapper.AiTaskMapper;
import com.hhk.picturebacked.manager.upload.URLPictureUploadResouseImpl;
import com.hhk.picturebacked.model.dto.ai.AiRenderCreateRequest;
import com.hhk.picturebacked.model.dto.ai.AiRenderMessage;
import com.hhk.picturebacked.model.dto.ai.AiRenderTaskResponse;
import com.hhk.picturebacked.model.dto.picture.PictureUploadRequest;
import com.hhk.picturebacked.model.entity.AiTask;
import com.hhk.picturebacked.model.entity.Picture;
import com.hhk.picturebacked.model.entity.Product;
import com.hhk.picturebacked.model.entity.User;
import com.hhk.picturebacked.model.enums.AiTaskStatusEnum;
import com.hhk.picturebacked.model.enums.AiTaskTypeEnum;
import com.hhk.picturebacked.model.enums.AssetTypeEnum;
import com.hhk.picturebacked.model.enums.PlatformSpecEnum;
import com.hhk.picturebacked.model.vo.AiTaskVO;
import com.hhk.picturebacked.service.AiRenderService;
import com.hhk.picturebacked.service.PictureService;
import com.hhk.picturebacked.service.ProductService;
import com.hhk.picturebacked.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 渲染服务实现
 *
 * @author 30674
 * @description 针对表【ai_task(AI 渲染任务)】的数据库操作Service实现
 */
@Service
@Slf4j
public class AiRenderServiceImpl extends ServiceImpl<AiTaskMapper, AiTask>
        implements AiRenderService {

    /**
     * AI 任务轮询最大等待次数
     */
    private static final int MAX_POLL_TIMES = 40;

    /**
     * AI 任务轮询间隔（毫秒）
     */
    private static final long POLL_INTERVAL_MS = 3000L;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private AliYunImageEditApi aliYunImageEditApi;

    @Resource
    @Lazy
    private PictureService pictureService;

    @Resource
    @Lazy
    private ProductService productService;

    @Resource
    private UserService userService;

    @Override
    public Long createTask(AiRenderCreateRequest request, User loginUser) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        Long sourcePictureId = request.getSourcePictureId();
        ThrowUtils.throwIf(sourcePictureId == null || sourcePictureId <= 0, ErrorCode.PARAMS_ERROR, "原图不能为空");

        // 任务类型校验
        AiTaskTypeEnum typeEnum = AiTaskTypeEnum.getEnumByValue(request.getType());
        ThrowUtils.throwIf(typeEnum == null, ErrorCode.PARAMS_ERROR, "不支持的任务类型：" + request.getType());

        // 平台校验
        PlatformSpecEnum spec = PlatformSpecEnum.getEnumByValue(request.getPlatform());
        ThrowUtils.throwIf(spec == null, ErrorCode.PARAMS_ERROR, "不支持的平台：" + request.getPlatform());

        // 场景图必须指定模板
        if (typeEnum == AiTaskTypeEnum.SCENE) {
            ThrowUtils.throwIf(StrUtil.isBlank(request.getTemplate())
                            || aliYunImageEditApi.getPrompt(request.getTemplate()) == null,
                    ErrorCode.PARAMS_ERROR, "场景模板不合法");
        }

        // 查询原图并做权限校验
        Picture sourcePicture = pictureService.getById(sourcePictureId);
        ThrowUtils.throwIf(sourcePicture == null, ErrorCode.NOT_FOUND_ERROR, "原图不存在");
        Long spaceId = sourcePicture.getSpaceId() == null ? 0L : sourcePicture.getSpaceId();

        // 幂等：同一 源图+类型+平台+模板 已有未完成任务，直接返回
        String requestKey = buildRequestKey(request);
        AiTask existed = this.lambdaQuery()
                .eq(AiTask::getRequestKey, requestKey)
                .in(AiTask::getStatus, AiTaskStatusEnum.QUEUED.getValue(),
                        AiTaskStatusEnum.PROCESSING.getValue())
                .one();
        if (existed != null) {
            log.info("AI 渲染任务已存在，幂等返回, taskId = {}", existed.getId());
            return existed.getId();
        }

        // 组装任务
        AiTask aiTask = new AiTask();
        aiTask.setSpaceId(spaceId);
        aiTask.setProductId(sourcePicture.getProductId());
        aiTask.setSourcePictureId(sourcePictureId);
        aiTask.setType(typeEnum.getValue());
        aiTask.setPlatform(spec.getValue());
        aiTask.setAssetType(resolveAssetType(request, typeEnum));
        aiTask.setTemplate(request.getTemplate());
        aiTask.setStatus(AiTaskStatusEnum.QUEUED.getValue());
        aiTask.setRequestKey(requestKey);
        aiTask.setUserId(loginUser.getId());
        aiTask.setParamJson(JSONUtil.toJsonStr(buildParamSnapshot(request, sourcePicture)));
        aiTask.setCreateTime(new Date());
        aiTask.setUpdateTime(new Date());
        boolean save = this.save(aiTask);
        ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR, "创建 AI 任务失败");

        // 发 MQ，提交即返回，不阻塞调用方
        AiRenderMessage message = new AiRenderMessage();
        message.setTaskId(aiTask.getId());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.AI_RENDER_EXCHANGE,
                RabbitMQConfig.AI_RENDER_ROUTING_KEY,
                message,
                msg -> {
                    // 消息持久化，服务重启不丢任务
                    msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return msg;
                }
        );
        log.info("AI 渲染任务已提交, taskId = {}, type = {}, platform = {}", aiTask.getId(), typeEnum.getValue(), spec.getValue());
        return aiTask.getId();
    }

    @Override
    public void executeTask(Long taskId) {
        if (taskId == null || taskId <= 0) {
            return;
        }
        AiTask aiTask = this.getById(taskId);
        if (aiTask == null) {
            log.warn("AI 渲染任务不存在, taskId = {}", taskId);
            return;
        }
        // 幂等：已成功的任务直接返回
        if (aiTask.getStatus() != null
                && aiTask.getStatus() == AiTaskStatusEnum.QUEUED.getValue()) {
            // 继续处理
        } else {
            log.info("AI 渲染任务状态非排队中，跳过, taskId = {}, status = {}", taskId, aiTask.getStatus());
            return;
        }

        // 标记处理中
        AiTask processing = new AiTask();
        processing.setId(taskId);
        processing.setStatus(AiTaskStatusEnum.PROCESSING.getValue());
        processing.setUpdateTime(new Date());
        this.updateById(processing);

        try {
            Picture sourcePicture = pictureService.getById(aiTask.getSourcePictureId());
            if (sourcePicture == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "原图不存在");
            }

            // 构造 prompt
            AiTaskTypeEnum typeEnum = AiTaskTypeEnum.getEnumByValue(aiTask.getType());
            String prompt = resolvePrompt(typeEnum, aiTask.getTemplate());
            if (StrUtil.isBlank(prompt)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "渲染指令为空");
            }

            // 创建 AI 任务并轮询结果
            AiRenderTaskResponse createResponse = aliYunImageEditApi.createEditTask(sourcePicture.getUrl(), prompt);
            String aiTaskId = createResponse.getOutput() == null ? null : createResponse.getOutput().getTaskId();
            ThrowUtils.throwIf(StrUtil.isBlank(aiTaskId), ErrorCode.OPERATION_ERROR, "AI 未返回任务 id");

            // 回填第三方任务 id
            AiTask aiTaskIdUpdate = new AiTask();
            aiTaskIdUpdate.setId(taskId);
            aiTaskIdUpdate.setAiTaskId(aiTaskId);
            aiTaskIdUpdate.setUpdateTime(new Date());
            this.updateById(aiTaskIdUpdate);

            // 轮询等待结果
            AiRenderTaskResponse result = pollTaskResult(aiTaskId);
            ThrowUtils.throwIf(result == null || result.getOutput() == null,
                    ErrorCode.OPERATION_ERROR, "AI 任务无返回结果");
            ThrowUtils.throwIf(!"SUCCEEDED".equals(result.getOutput().getTaskStatus()),
                    ErrorCode.OPERATION_ERROR, "AI 任务执行失败：" + describeFailure(result));

            String imageUrl = result.getOutput().firstImageUrl();
            ThrowUtils.throwIf(StrUtil.isBlank(imageUrl), ErrorCode.OPERATION_ERROR, "AI 未返回图片地址");

            // 产物落库：重新走图片上传流程，落到 COS 并生成 picture 记录
            Long resultPictureId = saveResultPicture(sourcePicture, aiTask, imageUrl);

            // 任务标记成功
            AiTask success = new AiTask();
            success.setId(taskId);
            success.setStatus(AiTaskStatusEnum.SUCCESS.getValue());
            success.setResultPictureId(resultPictureId);
            success.setUpdateTime(new Date());
            success.setFinishTime(new Date());
            this.updateById(success);
            log.info("AI 渲染任务完成, taskId = {}, resultPictureId = {}", taskId, resultPictureId);

        } catch (Exception e) {
            log.error("AI 渲染任务失败, taskId = {}", taskId, e);
            // 标记失败，可人工重试
            AiTask failed = new AiTask();
            failed.setId(taskId);
            failed.setStatus(AiTaskStatusEnum.FAILED.getValue());
            failed.setErrorMessage(StrUtil.sub(e.getMessage(), 0, 500));
            failed.setUpdateTime(new Date());
            failed.setFinishTime(new Date());
            this.updateById(failed);
            throw e;
        }
    }

    @Override
    public AiTaskVO getTaskVO(Long taskId, User loginUser) {
        ThrowUtils.throwIf(taskId == null || taskId <= 0, ErrorCode.PARAMS_ERROR);
        AiTask aiTask = this.getById(taskId);
        ThrowUtils.throwIf(aiTask == null, ErrorCode.NOT_FOUND_ERROR, "任务不存在");
        AiTaskVO vo = AiTaskVO.objToVo(aiTask);
        // 补原图与产物 url，方便前端直接预览
        Picture source = pictureService.getById(aiTask.getSourcePictureId());
        if (source != null) {
            vo.setSourcePictureUrl(source.getUrl());
        }
        if (aiTask.getResultPictureId() != null) {
            Picture result = pictureService.getById(aiTask.getResultPictureId());
            if (result != null) {
                vo.setResultPictureUrl(result.getUrl());
            }
        }
        return vo;
    }

    @Override
    public void retryTask(Long taskId, User loginUser) {
        ThrowUtils.throwIf(taskId == null || taskId <= 0, ErrorCode.PARAMS_ERROR);
        AiTask aiTask = this.getById(taskId);
        ThrowUtils.throwIf(aiTask == null, ErrorCode.NOT_FOUND_ERROR, "任务不存在");
        // 只有失败的任务能重试
        ThrowUtils.throwIf(aiTask.getStatus() == null
                        || aiTask.getStatus() != AiTaskStatusEnum.FAILED.getValue(),
                ErrorCode.PARAMS_ERROR, "只有失败的任务可以重试");
        // 重置为排队中并重新发消息
        AiTask reset = new AiTask();
        reset.setId(taskId);
        reset.setStatus(AiTaskStatusEnum.QUEUED.getValue());
        reset.setErrorMessage(null);
        reset.setUpdateTime(new Date());
        this.updateById(reset);

        AiRenderMessage message = new AiRenderMessage();
        message.setTaskId(taskId);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.AI_RENDER_EXCHANGE,
                RabbitMQConfig.AI_RENDER_ROUTING_KEY,
                message,
                msg -> {
                    msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return msg;
                }
        );
    }

    @Override
    public String buildRequestKey(AiRenderCreateRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append(request.getSourcePictureId()).append("_");
        sb.append(request.getType()).append("_");
        sb.append(StrUtil.isBlank(request.getPlatform()) ? "COMMON" : request.getPlatform()).append("_");
        sb.append(StrUtil.blankToDefault(request.getTemplate(), "NONE"));
        return sb.toString();
    }

    @Override
    public com.baomidou.mybatisplus.extension.plugins.pagination.Page<AiTaskVO> listTaskPage(Long spaceId, Integer status,
                                                                                      long pageNum, long pageSize) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<AiTask> taskPage = this.lambdaQuery()
                .eq(AiTask::getSpaceId, spaceId)
                .eq(status != null, AiTask::getStatus, status)
                .orderByDesc(AiTask::getCreateTime)
                .page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize));
        List<AiTaskVO> voList = taskPage.getRecords().stream()
                .map(AiTaskVO::objToVo)
                .collect(Collectors.toList());
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<AiTaskVO> voPage =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                        taskPage.getCurrent(), taskPage.getSize(), taskPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 轮询 AI 任务直到终态
     */
    private AiRenderTaskResponse pollTaskResult(String aiTaskId) {
        for (int i = 0; i < MAX_POLL_TIMES; i++) {
            AiRenderTaskResponse response = aliYunImageEditApi.getEditTask(aiTaskId);
            if (response == null || response.getOutput() == null) {
                return null;
            }
            String status = response.getOutput().getTaskStatus();
            if ("SUCCEEDED".equals(status) || "FAILED".equals(status)
                    || "CANCELED".equals(status) || "UNKNOWN".equals(status)) {
                return response;
            }
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return response;
            }
        }
        log.warn("AI 任务轮询超时, aiTaskId = {}", aiTaskId);
        return null;
    }

    /**
     * 产物落库：下载 AI 图 -> 上传 COS -> 落 picture 记录
     */
    private Long saveResultPicture(Picture sourcePicture, AiTask aiTask, String imageUrl) {
        PictureUploadRequest uploadRequest = new PictureUploadRequest();
        // 继承原图的空间与商品归属
        uploadRequest.setSpaceId(sourcePicture.getSpaceId());
        uploadRequest.setProductId(sourcePicture.getProductId());
        uploadRequest.setAssetType(aiTask.getAssetType());
        uploadRequest.setPlatform(aiTask.getPlatform());
        uploadRequest.setLang(sourcePicture.getLang());
        // 标记为 AI 产物，走放宽体积的校验策略
        uploadRequest.setUploadFlag(URLPictureUploadResouseImpl.UPLOAD_FLAG_AI);
        // 命名：SKU_平台_类型_序号
        String sku = null;
        if (sourcePicture.getProductId() != null) {
            Product product = productService.getById(sourcePicture.getProductId());
            if (product != null) {
                sku = product.getSku();
            }
        }
        uploadRequest.setPicName(buildResultName(sku, aiTask, sourcePicture));

        User taskUser = aiTask.getUserId() == null ? null : userService.getById(aiTask.getUserId());
        // AI 产物走专用上传：强制待审核，不允许管理员自动过审
        Picture saved = pictureService.uploadAiResultPicture(imageUrl, uploadRequest, taskUser);
        // 标记来源原图，形成版本树
        Picture source = new Picture();
        source.setId(saved.getId());
        source.setSourceId(aiTask.getSourcePictureId());
        pictureService.updateById(source);
        return saved.getId();
    }

    /**
     * 生成产物名称
     */
    private String buildResultName(String sku, AiTask aiTask, Picture sourcePicture) {
        StringBuilder sb = new StringBuilder();
        sb.append(StrUtil.isBlank(sku) ? "SKU" : sku).append("_");
        sb.append(aiTask.getPlatform()).append("_");
        sb.append(aiTask.getAssetType()).append("_");
        // 同类型同平台已有几张，用序号区分
        long existCount = pictureService.lambdaQuery()
                .eq(Picture::getSourceId, aiTask.getSourcePictureId())
                .eq(Picture::getPlatform, aiTask.getPlatform())
                .eq(Picture::getAssetType, aiTask.getAssetType())
                .count();
        sb.append(existCount + 1);
        return sb.toString();
    }

    /**
     * 解析 prompt
     */
    private String resolvePrompt(AiTaskTypeEnum typeEnum, String template) {
        if (typeEnum == AiTaskTypeEnum.WHITE_BG) {
            return aliYunImageEditApi.getPrompt("WHITE");
        }
        if (typeEnum == AiTaskTypeEnum.REMOVE_BG) {
            return aliYunImageEditApi.getPrompt("TRANSPARENT");
        }
        if (typeEnum == AiTaskTypeEnum.SCENE) {
            return aliYunImageEditApi.getPrompt(template);
        }
        return null;
    }

    /**
     * 推断产物素材类型
     */
    private String resolveAssetType(AiRenderCreateRequest request, AiTaskTypeEnum typeEnum) {
        if (StrUtil.isNotBlank(request.getAssetType())) {
            return request.getAssetType();
        }
        switch (typeEnum) {
            case WHITE_BG:
                return AssetTypeEnum.WHITE.getValue();
            case SCENE:
                return AssetTypeEnum.SCENE.getValue();
            case REMOVE_BG:
                return AssetTypeEnum.WHITE.getValue();
            default:
                return AssetTypeEnum.MAIN.getValue();
        }
    }

    /**
     * 参数快照
     */
    private Map<String, Object> buildParamSnapshot(AiRenderCreateRequest request, Picture sourcePicture) {
        Map<String, Object> map = new HashMap<>();
        map.put("sourcePictureId", request.getSourcePictureId());
        map.put("type", request.getType());
        map.put("platform", request.getPlatform());
        map.put("template", request.getTemplate());
        map.put("assetType", request.getAssetType());
        map.put("lang", request.getLang());
        map.put("sourceUrl", sourcePicture.getUrl());
        map.put("sourceWidth", sourcePicture.getPicWidth());
        map.put("sourceHeight", sourcePicture.getPicHeight());
        return map;
    }

    /**
     * 描述失败原因
     */
    private String describeFailure(AiRenderTaskResponse response) {
        if (response.getOutput() == null) {
            return response.getMessage();
        }
        String msg = response.getOutput().firstErrorMessage();
        return StrUtil.isBlank(msg) ? response.getMessage() : msg;
    }
}
