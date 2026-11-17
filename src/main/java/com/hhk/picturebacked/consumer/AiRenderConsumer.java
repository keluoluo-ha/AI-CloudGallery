package com.hhk.picturebacked.consumer;

import com.hhk.picturebacked.model.dto.ai.AiRenderMessage;
import com.hhk.picturebacked.service.AiRenderService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * AI 渲染消费者
 * <p>
 * 与浏览量消费者的关键差异：
 * 浏览量失败可以直接 ack 丢弃（统计指标，丢一条无所谓）；
 * AI 任务失败必须保留 —— 这里捕获异常后 nack 并 requeue，让任务重新排队，
 * 同时 ai_task 表里已标记为失败，运营可在后台看到并手动重试。
 * </p>
 */
@Component
@Slf4j
public class AiRenderConsumer {

    @Resource
    private AiRenderService aiRenderService;

    @RabbitListener(queues = "ai.render.queue", containerFactory = "aiRenderListenerContainerFactory")
    public void onMessage(AiRenderMessage renderMessage, Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            if (renderMessage == null || renderMessage.getTaskId() == null) {
                log.warn("AI 渲染消息格式非法, body = {}", new String(message.getBody(), "UTF-8"));
                // 格式非法的消息直接丢弃，重试也没用
                channel.basicAck(deliveryTag, false);
                return;
            }
            aiRenderService.executeTask(renderMessage.getTaskId());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("AI 渲染任务处理失败, taskId = {}", renderMessage == null ? null : renderMessage.getTaskId(), e);
            // 任务已落库为失败状态，这里 ack 掉避免无限重试堆积；
            // 运营可在后台看到失败任务并手动重试
            channel.basicAck(deliveryTag, false);
        }
    }
}
