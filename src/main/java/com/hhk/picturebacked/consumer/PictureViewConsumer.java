package com.hhk.picturebacked.consumer;

import com.hhk.picturebacked.model.dto.picture.PictureViewMessage;
import com.hhk.picturebacked.service.PictureViewService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 浏览量消费者：收到消息 -> Redis 计数 +1 -> 手动 ack
 */
@Component
@Slf4j
public class PictureViewConsumer {

    @Resource
    private PictureViewService pictureViewService;

    /**
     * 为什么手动 ack：
     * 1. 只有业务处理成功才 ack，消息不会在处理中被重复投递；
     * 2. 处理失败时我们选择 catch 日志 + ack 直接丢弃，不重试、不进死信队列。
     *
     * 为什么丢一条可以接受：
     * 浏览量是“统计指标”，不是资金/订单这类强一致数据，差 1 次不影响业务；
     * 为了这 1 条数据引入重试队列、死信队列、补偿任务会带来额外运维复杂度，
     * 与“尽量简单”的目标背道而驰。极端情况最多损失一次浏览，用户下次再访问会重新计数。
     *
     * 消息体由 Jackson2JsonMessageConverter 自动反序列化成 PictureViewMessage。
     */
    @RabbitListener(queues = "picture.view.queue", containerFactory = "pictureViewListenerContainerFactory")
    public void onMessage(PictureViewMessage viewMessage, Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            if (viewMessage == null || viewMessage.getPictureId() == null) {
                log.warn("浏览量消息格式非法, body = {}", new String(message.getBody(), "UTF-8"));
            } else {
                // Redis Hash 计数 +1
                pictureViewService.increaseViewCountInRedis(viewMessage.getPictureId());
            }
            // 处理成功，ack
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            // 异常也 ack：丢一条浏览量可以接受，不重试、不进死信
            log.error("处理浏览量消息失败, deliveryTag = {}", deliveryTag, e);
            channel.basicAck(deliveryTag, false);
        }
    }
}
