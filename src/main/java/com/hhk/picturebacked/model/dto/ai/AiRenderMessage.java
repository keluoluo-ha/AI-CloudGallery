package com.hhk.picturebacked.model.dto.ai;

import lombok.Data;

import java.io.Serializable;

/**
 * AI 渲染消息体
 * <p>
 * 走 RabbitMQ 时只带任务 id，具体参数从 ai_task 表读，
 * 避免消息体过大，也便于任务表排查问题。
 * </p>
 */
@Data
public class AiRenderMessage implements Serializable {

    /**
     * ai_task 表的任务 id
     */
    private Long taskId;

    private static final long serialVersionUID = 1L;
}
