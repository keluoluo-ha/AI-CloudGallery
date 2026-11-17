package com.hhk.picturebacked.controller;

import com.hhk.picturebacked.api.Aliyunimage.AliYunImageEditApi;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hhk.picturebacked.auth.annotation.SaSpaceCheckPermission;
import com.hhk.picturebacked.auth.model.SpaceUserPermissionConstant;
import com.hhk.picturebacked.commom.BaseResponse;
import com.hhk.picturebacked.commom.ResultUtils;
import com.hhk.picturebacked.exception.ErrorCode;
import com.hhk.picturebacked.exception.ThrowUtils;
import com.hhk.picturebacked.model.dto.ai.AiRenderCreateRequest;
import com.hhk.picturebacked.model.entity.User;
import com.hhk.picturebacked.model.vo.AiTaskVO;
import com.hhk.picturebacked.service.AiRenderService;
import com.hhk.picturebacked.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * AI 素材渲染
 * <p>
 * 提交即返回，异步产出，前端轮询任务状态。
 * </p>
 */
@RestController
@RequestMapping("/ai/render")
@Slf4j
public class AiRenderController {

    @Resource
    private AiRenderService aiRenderService;

    @Resource
    private AliYunImageEditApi aliYunImageEditApi;

    @Resource
    private UserService userService;

    /**
     * 提交渲染任务
     */
    @PostMapping("/create")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Long> createTask(@RequestBody AiRenderCreateRequest request, HttpServletRequest request2) {
        User loginUser = userService.getLoginUser(request2);
        Long taskId = aiRenderService.createTask(request, loginUser);
        return ResultUtils.success(taskId);
    }

    /**
     * 查询任务状态
     */
    @GetMapping("/get")
    public BaseResponse<AiTaskVO> getTask(@RequestParam("taskId") Long taskId, HttpServletRequest request) {
        ThrowUtils.throwIf(taskId == null || taskId <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(aiRenderService.getTaskVO(taskId, loginUser));
    }

    /**
     * 重试失败的任务
     */
    @PostMapping("/retry")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> retryTask(@RequestParam("taskId") Long taskId, HttpServletRequest request) {
        ThrowUtils.throwIf(taskId == null || taskId <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        aiRenderService.retryTask(taskId, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 场景模板列表
     */
    @GetMapping("/template/list")
    public BaseResponse<Map<String, String>> listTemplate() {
        return ResultUtils.success(aliYunImageEditApi.listSceneTemplates());
    }

    /**
     * 空间下的 AI 任务列表
     */
    @GetMapping("/list/page")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<Page<AiTaskVO>> listTaskPage(@RequestParam("spaceId") Long spaceId,
                                                     @RequestParam(value = "status", required = false) Integer status,
                                                     @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
                                                     @RequestParam(value = "pageSize", defaultValue = "20") long pageSize) {
        ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(pageSize > 100, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(aiRenderService.listTaskPage(spaceId, status, pageNum, pageSize));
    }
}
