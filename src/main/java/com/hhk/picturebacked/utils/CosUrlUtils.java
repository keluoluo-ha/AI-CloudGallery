package com.hhk.picturebacked.utils;

import cn.hutool.core.util.StrUtil;

/**
 * COS 图片 URL 工具类
 */
public final class CosUrlUtils {

    private CosUrlUtils() {
    }

    /**
     * 规范化对象存储访问地址（修复双斜杠、无后缀等问题）
     */
    public static String normalizeAccessUrl(String url) {
        if (StrUtil.isBlank(url)) {
            return url;
        }
        String normalized = url.replaceAll("(?<!:)//+", "/");
        if (normalized.endsWith(".")) {
            normalized = normalized + "webp";
        }
        return normalized;
    }

    /**
     * 从完整访问地址解析 COS 对象 key
     */
    public static String extractObjectKey(String url, String host) {
        if (StrUtil.isBlank(url)) {
            return url;
        }
        if (StrUtil.isNotBlank(host) && url.startsWith(host)) {
            String path = url.substring(host.length());
            return StrUtil.removePrefix(path, "/");
        }
        return StrUtil.removePrefix(url, "/");
    }

    /**
     * 根据 key 拼接访问地址
     */
    public static String buildAccessUrl(String host, String key) {
        if (StrUtil.isBlank(host) || StrUtil.isBlank(key)) {
            return key;
        }
        String cleanKey = StrUtil.removePrefix(key, "/");
        String cleanHost = StrUtil.removeSuffix(host, "/");
        return cleanHost + "/" + cleanKey;
    }
}
