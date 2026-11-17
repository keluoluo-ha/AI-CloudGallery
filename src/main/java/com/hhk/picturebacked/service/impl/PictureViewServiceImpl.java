package com.hhk.picturebacked.service.impl;

import com.hhk.picturebacked.config.RabbitMQConfig;
import com.hhk.picturebacked.exception.ErrorCode;
import com.hhk.picturebacked.exception.ThrowUtils;
import com.hhk.picturebacked.model.dto.picture.PictureViewMessage;
import com.hhk.picturebacked.model.entity.Picture;
import com.hhk.picturebacked.model.vo.PictureViewRankVO;
import com.hhk.picturebacked.service.PictureService;
import com.hhk.picturebacked.service.PictureViewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 图片浏览量服务实现
 */
@Service
@Slf4j
public class PictureViewServiceImpl implements PictureViewService {

    /**
     * 防刷 key 前缀：view:limit:{pictureId}:{userId}
     */
    private static final String VIEW_LIMIT_PREFIX = "view:limit:";

    /**
     * 计数 Hash 的 key 前缀：picture:view:{yyyyMMdd}
     * 这是「当天增量缓冲区」，每 30 秒被刷库任务取走并删除
     */
    private static final String VIEW_COUNT_PREFIX = "picture:view:";

    /**
     * 榜单 ZSET 的 key 前缀：picture:view:rank:{yyyyWW}（按周分桶）
     * member = pictureId，score = 累计浏览量。
     *
     * 为什么单独用 ZSET：
     * 1. Redis Hash 不能按 value 排序，榜单要「按浏览量倒序取 TOP10」必须用 ZSET
     *    （一条 ZREVRANGE 直接搞定，不用全量取回内存排序）；
     * 2. 上面的 Hash 是「当天增量」，刷库时会被删掉，如果榜单读它，
     *    30 秒里基本都是空的。
     *
     * 为什么按周分桶：周榜只关心「这一周」的量，用 yyyyWW 分桶天然就是一周一个 key，
     * 不需要跨多天合并，也不用做缓存和冷启动降级。
     */
    private static final String VIEW_RANK_PREFIX = "picture:view:rank:";

    /**
     * 防刷有效期：1 小时
     */
    private static final long VIEW_LIMIT_TTL_HOURS = 1L;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 周榜 key 的日期格式：yyyyWW（第几周）
     */
    private static final DateTimeFormatter RANK_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy'W'ww");

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private PictureService pictureService;

    /**
     * 上报浏览：Redis SETNX 防刷 -> 发 MQ 消息 -> 立即返回（不更新数据库）
     */
    @Override
    public boolean reportView(Long pictureId, Long userId) {
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        // 1. 校验图片是否存在
        Picture picture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        // 2. Redis SETNX 防刷：1 小时内同一用户对同一张图只计一次
        String limitKey = VIEW_LIMIT_PREFIX + pictureId + ":" + (userId == null ? 0L : userId);
        Boolean first = stringRedisTemplate.opsForValue().setIfAbsent(limitKey, "1", VIEW_LIMIT_TTL_HOURS, TimeUnit.HOURS);
        if (Boolean.FALSE.equals(first)) {
            // 已经计过数了：直接返回成功，但不再发消息
            log.debug("浏览量防刷拦截, pictureId = {}, userId = {}", pictureId, userId);
            return false;
        }
        // 3. 通过防刷，发一条 MQ 消息（消息体只有 pictureId 和 userId）
        PictureViewMessage message = new PictureViewMessage();
        message.setPictureId(pictureId);
        message.setUserId(userId);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.VIEW_EXCHANGE,
                RabbitMQConfig.VIEW_ROUTING_KEY,
                message,
                msg -> {
                    // 消息持久化，RabbitMQ 重启后不丢
                    msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return msg;
                }
        );
        // 不做 confirm 回调：浏览量丢一条可以接受，避免引入额外的回调、重试、死信逻辑
        log.debug("浏览量消息已发送, pictureId = {}, userId = {}", pictureId, userId);
        return true;
    }

    /**
     * 消费者调用：Redis Hash 计数 +1
     * 同时维护一个 ZSET 累计榜（永不删除），供榜单接口读取
     */
    @Override
    public void increaseViewCountInRedis(Long pictureId) {
        if (pictureId == null || pictureId <= 0) {
            return;
        }
        String key = VIEW_COUNT_PREFIX + LocalDate.now().format(DATE_FORMATTER);
        HashOperations<String, String, String> hashOps = stringRedisTemplate.opsForHash();
        // field = pictureId，value = 当天的累计增量
        hashOps.increment(key, String.valueOf(pictureId), 1L);
        // ZSET：member = pictureId，score = 累计浏览量，用于按 value 排序取 TOP N
        stringRedisTemplate.opsForZSet().incrementScore(currentRankKey(), String.valueOf(pictureId), 1D);
    }

    /**
     * 当前周的榜单 key：picture:view:rank:{yyyyWW}
     */
    private String currentRankKey() {
        return VIEW_RANK_PREFIX + LocalDate.now().format(RANK_DATE_FORMATTER);
    }

    /**
     * 周榜 TOP N：直接读 Redis 的 ZSET 计数 key，按 score（浏览量）倒序取前 N
     * ZSET 天然支持按 value 排序，一条 ZREVRANGE 就够了，不用全量取回内存排序。
     * 按需求：不做多日合并、不缓存、不做冷启动降级。
     */
    @Override
    public List<PictureViewRankVO> getWeeklyRank(int topN) {
        int limit = topN <= 0 ? 10 : topN;
        Set<String> members = stringRedisTemplate.opsForZSet().reverseRange(currentRankKey(), 0, limit - 1L);
        List<PictureViewRankVO> list = new ArrayList<>();
        if (members == null || members.isEmpty()) {
            return list;
        }
        for (String member : members) {
            Double score = stringRedisTemplate.opsForZSet().score(currentRankKey(), member);
            PictureViewRankVO vo = new PictureViewRankVO();
            try {
                vo.setPictureId(Long.valueOf(member));
                vo.setViewCount(score == null ? 0L : score.longValue());
            } catch (NumberFormatException e) {
                log.warn("榜单数据格式异常, member = {}", member);
                continue;
            }
            list.add(vo);
        }
        return list;
    }
}
