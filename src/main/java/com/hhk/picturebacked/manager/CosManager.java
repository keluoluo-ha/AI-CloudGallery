package com.hhk.picturebacked.manager;

import cn.hutool.core.io.FileUtil;
import com.hhk.picturebacked.config.CosClientConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class CosManager {

    @Resource
    public CosClientConfig cosClientConfig;

    @Resource
    public COSClient cosClient;

    /**
     * 将本地文件上传到 COS
     */
    public PutObjectResult uploadCOS(String key, File file) throws CosClientException, CosServiceException {
        String objectKey = cn.hutool.core.util.StrUtil.removePrefix(key, "/");
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), objectKey, file);

        // 图片处理参数
        PicOperations picOperations = new PicOperations();
        picOperations.setIsPicInfo(1);
        List<PicOperations.Rule> ruleList = new ArrayList<>();

        // 1. 转 webp
        // 注意：PicOperations.Rule.fileId 是相对于「源对象所在目录」的路径，
        //      不能带目录前缀，否则 COS 会拼成 目录/目录/xxx.webp（路径重复）。
        //      这里只取文件名，产物就落在源对象同目录下。
        String webpKey = toWebpKey(objectKey);
        PicOperations.Rule webpRule = new PicOperations.Rule();
        webpRule.setBucket(cosClientConfig.getBucket());
        webpRule.setFileId(webpKey);
        webpRule.setRule("imageMogr2/format/webp");
        ruleList.add(webpRule);

        // 2. 生成缩略图 （只有文件>2KB才生成）
        if (file.length() > 2 * 1024) {
            String thumbnailKey = webpKey.replace(".webp", "_thumbnail.webp");
            PicOperations.Rule thumbnailRule = new PicOperations.Rule();
            thumbnailRule.setBucket(cosClientConfig.getBucket());
            thumbnailRule.setFileId(thumbnailKey);
            // 正确的缩略图规则：宽度等比缩放到300px（可自行修改尺寸）
            thumbnailRule.setRule("imageMogr2/thumbnail/300x/format/webp");
            ruleList.add(thumbnailRule);
        }

        // 设置规则
        picOperations.setRules(ruleList);
        putObjectRequest.setPicOperations(picOperations);

        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 下载文件
     */
    public COSObject downloadCOS(String key) throws CosClientException, CosServiceException, IOException {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        return cosClient.getObject(getObjectRequest);
    }

    /**
     * 删除文件
     */
    public void deleteObject(String key) throws CosClientException {
        String objectKey = cn.hutool.core.util.StrUtil.removePrefix(key, "/");
        cosClient.deleteObject(cosClientConfig.getBucket(), objectKey);
    }

    /**
     * 将对象 key 转为同目录下的 webp 文件名
     * <p>
     * PicOperations.Rule.fileId 只接受文件名（不含目录），
     * COS 会自动把它放到源对象所在的目录下。
     * </p>
     */
    private static String toWebpKey(String key) {
        String cleanKey = cn.hutool.core.util.StrUtil.removePrefix(key, "/");
        // 只保留文件名部分
        String fileName = cleanKey;
        int slashIndex = cleanKey.lastIndexOf('/');
        if (slashIndex >= 0) {
            fileName = cleanKey.substring(slashIndex + 1);
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            return fileName.substring(0, dotIndex) + ".webp";
        }
        return fileName + ".webp";
    }
}