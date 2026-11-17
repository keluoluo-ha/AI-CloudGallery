package com.hhk.picturebacked.task;

import com.hhk.picturebacked.mapper.PictureViewMapper;
import com.hhk.picturebacked.model.dto.picture.PictureViewCountItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 浏览量刷库定时任务：每 30 秒把 Redis 里的增量批量刷回 MySQL
 *
 * 为什么批量：
 * 逐条 update 会产生 N 条 SQL，30 秒内几千条就是几千次网络往返 + 行锁竞争；
 * 一条带 CASE WHEN 的 UPDATE 只发一次请求，数据库内部一次算完，效率差几个数量级。
 */
@Component
@Slf4j
public class PictureViewFlushTask {

    /**
     * 计数 Hash 的 key 前缀：picture:view:{yyyyMMdd}
     * 这是「当天增量缓冲区」，刷库时被取走并删除
     */
    private static final String VIEW_COUNT_PREFIX = "picture:view:";

    /**
     * 刷库中转 key 前缀：用 RENAME 原子地把当天 key 挪走，
     * 避免「读到一半消费者又写进来」导致增量丢失或重复
     */
    private static final String VIEW_FLUSH_PREFIX = "picture:view:flush:";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private PictureViewMapper pictureViewMapper;

    /**
     * 每 30 秒执行一次
     */
    @Scheduled(fixedDelay = 30_000L)
    public void flushViewCount() {
        // 处理今天和昨天两个 key：
        // 跨零点时昨天残留的增量如果没人管就永远刷不进库了，顺手一起处理掉
        flushOneKey(LocalDate.now());
        flushOneKey(LocalDate.now().minusDays(1));
    }

    /**
     * 刷一个 key 的增量
     */
    private void flushOneKey(LocalDate date) {
        String sourceKey = VIEW_COUNT_PREFIX + date.format(DATE_FORMATTER);
        String flushKey = VIEW_FLUSH_PREFIX + System.currentTimeMillis();
        Map<Object, Object> entries = null;
        try {
            // 没有增量就不做任何事
            Boolean exists = stringRedisTemplate.hasKey(sourceKey);
            if (!Boolean.TRUE.equals(exists)) {
                return;
            }
            // RENAME 是原子操作：把当天 key 整体挪到中转 key，
            // 消费者后续的 HINCRBY 会写回新的 sourceKey，不会污染这次要刷的数据。
            stringRedisTemplate.rename(sourceKey, flushKey);
            entries = stringRedisTemplate.opsForHash().entries(flushKey);
        } catch (Exception e) {
            log.error("读取浏览量增量失败, key = {}", sourceKey, e);
            return;
        }
        if (entries == null || entries.isEmpty()) {
            // 空数据，删掉中转 key 即可
            stringRedisTemplate.delete(flushKey);
            return;
        }
        // 解析成 图片id -> 增量
        List<PictureViewCountItem> items = new ArrayList<>(entries.size());
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            try {
                items.add(new PictureViewCountItem(
                        Long.valueOf(String.valueOf(entry.getKey())),
                        Long.valueOf(String.valueOf(entry.getValue()))));
            } catch (NumberFormatException e) {
                log.warn("浏览量增量数据格式异常, key = {}, value = {}", entry.getKey(), entry.getValue());
            }
        }
        if (items.isEmpty()) {
            stringRedisTemplate.delete(flushKey);
            return;
        }
        try {
            // 一条 SQL 批量刷库：
            // UPDATE picture SET view_count = view_count + CASE id WHEN 1 THEN 3 ... END WHERE id IN (1,2)
            int rows = pictureViewMapper.batchAddViewCount(items);
            log.info("浏览量刷库完成, key = {}, 影响行数 = {}, 图片数 = {}", sourceKey, rows, items.size());
            // 刷库成功，删除中转 key
            stringRedisTemplate.delete(flushKey);
        } catch (Exception e) {
            // 刷库失败：把增量重新塞回 Redis，避免丢数据
            log.error("浏览量刷库失败，增量回写 Redis, key = {}, 图片数 = {}", sourceKey, items.size(), e);
            try {
                HashOperations<String, String, String> hashOps = stringRedisTemplate.opsForHash();
                for (PictureViewCountItem item : items) {
                    hashOps.increment(sourceKey, String.valueOf(item.getPictureId()), item.getCount());
                }
                stringRedisTemplate.delete(flushKey);
            } catch (Exception ex) {
                log.error("浏览量增量回写 Redis 失败", ex);
            }
        }
    }
}
