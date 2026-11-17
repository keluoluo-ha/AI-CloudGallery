package com.hhk.picturebacked.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hhk.picturebacked.model.dto.ai.AiRenderCreateRequest;
import com.hhk.picturebacked.model.entity.AiTask;
import com.hhk.picturebacked.model.entity.User;
import com.hhk.picturebacked.model.vo.AiTaskVO;

/**
 * AI 渲染服务
 * <p>
 * 提交即返回：落 ai_task 表 + 发 MQ，消费者异步调 AI，
 * 产物落 picture 并回填 result_picture_id，前端轮询任务状态。
 * </p>
 */
public interface AiRenderService extends IService<AiTask> {

    /**
     * 提交渲染任务
     *
     * @param request    提交请求
     * @param loginUser  登录用户
     * @return 任务 id
     */
    Long createTask(AiRenderCreateRequest request, User loginUser);

    /**
     * 执行渲染任务（由 MQ 消费者调用）
     *
     * @param taskId 任务 id
     */
    void executeTask(Long taskId);

    /**
     * 查询任务详情
     *
     * @param taskId     任务 id
     * @param loginUser  登录用户
     */
    AiTaskVO getTaskVO(Long taskId, User loginUser);

    /**
     * 重试失败的任务
     *
     * @param taskId     任务 id
     * @param loginUser  登录用户
     */
    void retryTask(Long taskId, User loginUser);

    /**
     * 构建幂等键：源图 + 类型 + 平台 + 模板
     *
     * @param request 提交请求
     */
    String buildRequestKey(AiRenderCreateRequest request);

    /**
     * 查询空间下的任务列表
     *
     * @param spaceId  空间 id
     * @param status   任务状态，为空不过滤
     * @param pageNum  页码
     * @param pageSize 每页条数
     */
    com.baomidou.mybatisplus.extension.plugins.pagination.Page<AiTaskVO> listTaskPage(Long spaceId, Integer status,
                                                                               long pageNum, long pageSize);
}
