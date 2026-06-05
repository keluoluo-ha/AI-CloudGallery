package com.hhk.picturebacked.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.hhk.picturebacked.config.CosClientConfig;
import com.hhk.picturebacked.exception.BusinessException;
import com.hhk.picturebacked.exception.ErrorCode;
import com.hhk.picturebacked.manager.CosManager;
import com.hhk.picturebacked.model.dto.picture.UploadPictureResult;
import com.hhk.picturebacked.utils.CosUrlUtils;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public abstract class PictureUploadResouse {

    @Resource
    private CosClientConfig cosClientConfig;


    @Resource
    public CosManager cosManager;

    /**
     * 上传图片
     * @param inputResouse
     * @param uploadPathPrefix
     * @return
     */
    public UploadPictureResult uploadPicture(Object inputResouse,String uploadPathPrefix){
//        1.文件校验
         validPicture(inputResouse);
        System.out.println("检验成功");
        //2.整合图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originalFilename = getOriginalFilename(inputResouse);

        String suffix = FileUtil.getSuffix(originalFilename);
        if (StrUtil.isBlank(suffix)) {
            suffix = "jpg";
        }
        String uploadfilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, suffix);
        String uploadpath = String.format("%s/%s", uploadPathPrefix, uploadfilename);
        File file=null;

        try {
            //3.创建临时文件
            file=File.createTempFile(uploadpath,null);
            transferToFile(inputResouse,file);
            PutObjectResult putObjectResult = cosManager.uploadCOS(uploadpath, file);
            //获取图片信息
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            //封装压缩图返回结果
            List<CIObject> objectList = processResults.getObjectList();
            if(CollUtil.isNotEmpty(objectList)){
                CIObject compressciObject = objectList.get(0);
                CIObject thumbnailciObject = compressciObject;
                if(objectList.size()>1){
                    thumbnailciObject=objectList.get(1);
                }
                return buildResult(originalFilename,compressciObject,thumbnailciObject,imageInfo);
            }
            //返回封装结果
            return buildResult(uploadpath,file,originalFilename,imageInfo);

        } catch (Exception e) {
            log.error("图片上传到对象存储失败",e);
           throw new BusinessException(ErrorCode.OPERATION_ERROR,"上传失败");
        } finally {
            //临时文件清除
            deleteTemplate(file);
        }

    }


    /**
     * 封装从压缩图片中获取信息
     *
     * @param originFilename
     * @param compressedCiObject
     * @param thumbnailciObject
     * @return
     */
    private UploadPictureResult buildResult(String originFilename, CIObject compressedCiObject, CIObject thumbnailciObject,ImageInfo imageInfo) {
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        int picWidth = compressedCiObject.getWidth();
        int picHeight = compressedCiObject.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(compressedCiObject.getFormat());
        uploadPictureResult.setPicSize(compressedCiObject.getSize().longValue());
        uploadPictureResult.setPicColor(imageInfo.getAve());

        // 设置图片为压缩后的地址
        uploadPictureResult.setUrl(CosUrlUtils.buildAccessUrl(cosClientConfig.getHost(), compressedCiObject.getKey()));
        uploadPictureResult.setThumbnailUrl(CosUrlUtils.buildAccessUrl(cosClientConfig.getHost(), thumbnailciObject.getKey()));
        return uploadPictureResult;
    }

    /**
     * 封装UploadPictureResult对象
     * @param uploadpath
     * @param file
     * @param originalFilename
     * @param imageInfo
     * @return
     */
    private UploadPictureResult buildResult(String uploadpath, File file, String originalFilename,ImageInfo imageInfo) {

        UploadPictureResult uploadPictureResult =new UploadPictureResult();
        //封装图片信息
        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setPicColor(imageInfo.getAve());


        uploadPictureResult.setUrl(CosUrlUtils.buildAccessUrl(cosClientConfig.getHost(), uploadpath));
        //返回图片结果
        return uploadPictureResult;
    }

    /**
     * 获取文件名接口
     * @param inputResourse
     * @return
     */
    public abstract String getOriginalFilename(Object inputResourse);

    /**
     * 文件类型转换
     * @param inputResourse
     */
    public abstract void transferToFile(Object inputResourse,File file) throws IOException;

    /**
     * 图片检验接口
     * @param inputResouse
     */
    public abstract void validPicture(Object inputResouse);




    /**
     * 文件清除
     * @param file
     */
    public void deleteTemplate(File file){
        if(file==null) return;
        boolean delete = file.delete();
        if(!delete){
            log.error("file delete error={}",file.getAbsolutePath());

        }
    }




}
