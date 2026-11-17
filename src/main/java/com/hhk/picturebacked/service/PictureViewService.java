package com.hhk.picturebacked.service;

import com.hhk.picturebacked.model.vo.PictureViewRankVO;

import java.util.List;

/**
 * 图片浏览量服务
 * 写路径：防刷 -> 发 MQ -> 消费者 Redis 计数 -> 定时任务批量刷库
 * 读路径：详情接口返回 DB 里的 view_count
 */
public interface PictureViewService {

    /**
     * 上报一次浏览
     *
     * @param pictureId 图片 id
     * @param userId    用户 id（未登录时使用 IP 哈希兜底）
     * @return true 表示这次计数已生效；false 表示被防刷拦截（不再发消息）
     */
    boolean reportView(Long pictureId, Long userId);

    /**
     * 消费者使用：Redis Hash 计数 +1
     *
     * @param pictureId 图片 id
     */
    void increaseViewCountInRedis(Long pictureId);

    /**
     * 周榜 TOP N：直接读 Redis 计数 key，按 value 倒序取前 N
     * 这里只排当天这一天的计数（按需求不做多日合并、不缓存、不做冷启动降级）
     *
     * @param topN 取前多少条
     * @return 图片 id + 浏览量
     */
    List<PictureViewRankVO> getWeeklyRank(int topN);
}
