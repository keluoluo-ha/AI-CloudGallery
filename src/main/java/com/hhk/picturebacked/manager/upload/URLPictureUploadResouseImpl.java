package com.hhk.picturebacked.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.*;
import com.hhk.picturebacked.exception.ErrorCode;
import com.hhk.picturebacked.exception.ThrowUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;


@Service
public class URLPictureUploadResouseImpl extends PictureUploadResouse {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private static final List<String> ALLOW_FORMAT_LIST = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif"
    );

    @Override
    public String getOriginalFilename(Object inputResourse) {
        String url = (String) inputResourse;
        String name = FileUtil.getName(url);
        // 去掉查询参数后再取文件名
        int queryIndex = name.indexOf('?');
        if (queryIndex > 0) {
            name = name.substring(0, queryIndex);
        }
        return name;
    }

    @Override
    public void transferToFile(Object inputResourse, File file) {
        String url = (String) inputResourse;
        try {
            HttpResponse response = HttpRequest.get(url)
                    .header(Header.USER_AGENT, USER_AGENT)
                    .timeout(30000)
                    .execute();
            if (!response.isOk()) {
                throw new RuntimeException("下载失败，HTTP 状态码: " + response.getStatus());
            }
            FileUtil.writeBytes(response.bodyBytes(), file);
            if (file.length() <= 0) {
                throw new RuntimeException("下载到的文件为空");
            }
        } catch (HttpException e) {
            throw new RuntimeException("下载图片失败: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("下载图片失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void validPicture(Object inputResourse) {
        String fileurl = (String) inputResourse;
        ThrowUtils.throwIf(StrUtil.isBlank(fileurl), ErrorCode.PARAMS_ERROR, "图片地址不能为空");
        try {
            new URL(fileurl);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        ThrowUtils.throwIf(!fileurl.startsWith("http://") && !fileurl.startsWith("https://"), ErrorCode.PARAMS_ERROR, "仅支持 http/https 链接");

        HttpResponse response = null;
        try {
            response = HttpRequest.head(fileurl)
                    .header(Header.USER_AGENT, USER_AGENT)
                    .timeout(15000)
                    .execute();
            // 部分图床不支持 HEAD，改用 GET 仅读取响应头
            if (!response.isOk()) {
                response.close();
                response = HttpRequest.get(fileurl)
                        .header(Header.USER_AGENT, USER_AGENT)
                        .header("Range", "bytes=0-0")
                        .timeout(15000)
                        .execute();
            }
            ThrowUtils.throwIf(!response.isOk(), ErrorCode.PARAMS_ERROR, "图片地址无法访问");

            String contentType = response.header(Header.CONTENT_TYPE);
            if (StrUtil.isNotBlank(contentType)) {
                String mimeType = contentType.split(";")[0].trim().toLowerCase();
                ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(mimeType), ErrorCode.PARAMS_ERROR, "不支持的图片类型: " + mimeType);
            }
            String contentLength = response.header(Header.CONTENT_LENGTH);
            if (StrUtil.isNotBlank(contentLength)) {
                long length = Long.parseLong(contentLength);
                final long maxSize = 2 * 1024 * 1024L;
                ThrowUtils.throwIf(length > maxSize, ErrorCode.PARAMS_ERROR, "文件大小不能超过 2MB");
            }
        } catch (NumberFormatException e) {
            // Content-Length 解析失败时忽略，下载阶段再校验
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }
}
