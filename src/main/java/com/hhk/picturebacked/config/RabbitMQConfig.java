package com.hhk.picturebacked.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 浏览量 MQ 相关配置
 * 交换机：topic 类型，便于以后扩展多种 routing key
 * 队列 / 绑定：持久化，保证服务重启后队列和消息不丢
 */
@Configuration
public class RabbitMQConfig {

    /** 交换机名 */
    public static final String VIEW_EXCHANGE = "picture.view.exchange";
    /** 队列名 */
    public static final String VIEW_QUEUE = "picture.view.queue";
    /** routing key */
    public static final String VIEW_ROUTING_KEY = "picture.view";

    /**
     * AI 渲染交换机名
     * 与浏览量通道完全隔离：可靠性策略不同（浏览量可丢，AI 任务不可丢）
     */
    public static final String AI_RENDER_EXCHANGE = "ai.render.exchange";

    /**
     * AI 渲染队列名
     */
    public static final String AI_RENDER_QUEUE = "ai.render.queue";

    /**
     * AI 渲染 routing key
     */
    public static final String AI_RENDER_ROUTING_KEY = "ai.render";

    /**
     * topic 交换机（持久化）
     */
    @Bean
    public TopicExchange pictureViewExchange() {
        // name, durable(持久化), autoDelete
        return new TopicExchange(VIEW_EXCHANGE, true, false);
    }

    /**
     * 队列（持久化）
     */
    @Bean
    public Queue pictureViewQueue() {
        return new Queue(VIEW_QUEUE, true);
    }

    /**
     * 队列与交换机的绑定
     */
    @Bean
    public Binding pictureViewBinding() {
        return BindingBuilder.bind(pictureViewQueue()).to(pictureViewExchange()).with(VIEW_ROUTING_KEY);
    }

    /**
     * AI 渲染交换机（持久化）
     */
    @Bean
    public TopicExchange aiRenderExchange() {
        return new TopicExchange(AI_RENDER_EXCHANGE, true, false);
    }

    /**
     * AI 渲染队列（持久化）
     */
    @Bean
    public Queue aiRenderQueue() {
        return new Queue(AI_RENDER_QUEUE, true);
    }

    /**
     * AI 渲染队列绑定
     */
    @Bean
    public Binding aiRenderBinding() {
        return BindingBuilder.bind(aiRenderQueue()).to(aiRenderExchange()).with(AI_RENDER_ROUTING_KEY);
    }

    /**
     * 消息序列化：JSON，方便直接看消息内容
     *
     * 注意：Jackson2JsonMessageConverter 发送时会把类名写进 __TypeId__ 头，
     * 接收端靠这个头反序列化。默认只信任 java.util / java.lang，
     * 自定义 DTO 必须显式加进信任列表，否则消费端会报 Class not in trusted packages。
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages("java.util", "java.lang", "com.hhk.picturebacked");
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

    /**
     * 消费者容器工厂：手动 ack
     * ACK：只有业务处理成功才 basicAck；异常时我们选择直接 ack（丢弃），
     * 不做重试、不进死信队列——浏览量丢一条完全可以接受，为它引入重试/死信会带来更多复杂度。
     */
    @Bean("pictureViewListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory pictureViewListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        // 手动 ack
        factory.setAcknowledgeMode(org.springframework.amqp.core.AcknowledgeMode.MANUAL);
        // 消费能力：4~8 个消费者线程
        factory.setConcurrentConsumers(4);
        factory.setMaxConcurrentConsumers(8);
        // 预取，攒一批再处理
        factory.setPrefetchCount(200);
        return factory;
    }

    /**
     * AI 渲染消费者容器工厂
     * <p>
     * 与浏览量通道的关键差异：
     * 1. 浏览量通道失败直接 ack 丢弃（统计指标，丢一条无所谓）；
     *    AI 任务失败必须保留，所以这里关闭自动重试，由消费端根据异常类型决定
     *    「标记任务失败」还是「requeue 重试」。
     * 2. AI 出图单次 10~40s，IO 密集而非 CPU 密集，可以多线程并发；
     *    但预取不能大，避免任务在内存里堆积太久（任务有 TTL 语义）。
     * 3. 单条消费（prefetch=1），保证同一店铺的任务尽量顺序处理。
     * </p>
     */
    @Bean("aiRenderListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory aiRenderListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        // 手动 ack：成功 ack，失败由业务侧决定 requeue 还是标记失败
        factory.setAcknowledgeMode(org.springframework.amqp.core.AcknowledgeMode.MANUAL);
        // AI 调用是 IO 等待，可以适度并发
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(4);
        // 单条消费，避免任务长时间滞留在消费端
        factory.setPrefetchCount(1);
        // 不自动重试，失败由消费端显式处理
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
