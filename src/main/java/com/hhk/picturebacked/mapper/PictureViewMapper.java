package com.hhk.picturebacked.mapper;

import com.hhk.picturebacked.model.dto.picture.PictureViewCountItem;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 浏览量刷库 Mapper
 * 只有一条批量 UPDATE，避免逐条 update 打爆数据库
 */
public interface PictureViewMapper {

    /**
     * 批量累加浏览量
     * 生成形如：
     * UPDATE picture SET view_count = view_count + CASE id WHEN 1 THEN 3 WHEN 2 THEN 5 ELSE 0 END
     * WHERE id IN (1,2)
     *
     * @param items 增量列表
     * @return 受影响行数
     */
    @Update({
            "<script>",
            "UPDATE picture SET view_count = view_count + CASE id",
            "<foreach collection='items' item='it'> WHEN #{it.pictureId} THEN #{it.count}</foreach>",
            " ELSE 0 END WHERE id IN",
            "<foreach collection='items' item='it' open='(' separator=',' close=')'>#{it.pictureId}</foreach>",
            "</script>"
    })
    int batchAddViewCount(@Param("items") List<PictureViewCountItem> items);
}
