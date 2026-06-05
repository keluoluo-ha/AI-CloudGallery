package com.hhk.picturebacked.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.hhk.picturebacked.config.CosClientConfig;
import com.hhk.picturebacked.exception.ErrorCode;
import com.hhk.picturebacked.exception.ThrowUtils;
import com.hhk.picturebacked.model.dto.picture.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import java.util.*;

@Service
@Slf4j
@Deprecated
public class FileManager {

    @Resource
    private CosClientConfig cosClientConfig;


    @Resource
    public CosManager cosManager;

    /**
     * 上传图片
     * @param multipartFile
     * @param uploadPathPrefix
     * @return
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile,String uploadPathPrefix){
        validPicture(multipartFile);
        String uuid = RandomUtil.randomString(16);
        String originalFilename = multipartFile.getOriginalFilename();
        String uploadfilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, FileUtil.getSuffix(originalFilename));
        String uploadpath = String.format("/%s/%s", uploadPathPrefix, uploadfilename);
        File file=null;
        try {
            file=File.createTempFile(uploadpath,null);
            multipartFile.transferTo(file);
            PutObjectResult putObjectResult = cosManager.uploadCOS(uploadpath, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            UploadPictureResult uploadPictureResult =new UploadPictureResult();
            int picWidth = imageInfo.getWidth();
            int picHeight = imageInfo.getHeight();
            double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
            uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
            uploadPictureResult.setPicWidth(picWidth);
            uploadPictureResult.setPicHeight(picHeight);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadpath);
            return uploadPictureResult;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            deleteTemplate(file);
        }

    }

    /**
     * 上传图片url
     * @param fileurl
     * @param uploadPathPrefix
     * @return
     */
    public UploadPictureResult uploadPictureByUrl(String fileurl,String uploadPathPrefix){
        validPicture(fileurl);
        String uuid = RandomUtil.randomString(16);
        String originalFilename = FileUtil.mainName(fileurl);
        String uploadfilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, "jpg");
        String uploadpath = String.format("/%s/%s", uploadPathPrefix, uploadfilename);
        File file=null;
        try {
            file=File.createTempFile(uploadpath,null);
            HttpUtil.downloadFile(fileurl,file);
            PutObjectResult putObjectResult = cosManager.uploadCOS(uploadpath, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            UploadPictureResult uploadPictureResult =new UploadPictureResult();
            int picWidth = imageInfo.getWidth();
            int picHeight = imageInfo.getHeight();
            double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
            uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
            uploadPictureResult.setPicWidth(picWidth);
            uploadPictureResult.setPicHeight(picHeight);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadpath);
            return uploadPictureResult;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            deleteTemplate(file);
        }

    }

    /**
     * 图片URL检验
     * @param fileurl
     */
    public void validPicture(String fileurl){
        //1.校验是否为空
        ThrowUtils.throwIf(StrUtil.isBlank(fileurl),ErrorCode.PARAMS_ERROR);
        //2.检验URL格式
        try {
            new URL(fileurl);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        //3.检验URL的协议
        ThrowUtils.throwIf(!fileurl.startsWith("http://")&&!fileurl.startsWith("https://"),ErrorCode.PARAMS_ERROR);
        //4.发送HEAD请求验证文件是否存在
        HttpResponse response=null;
        try {
            response= HttpUtil.createRequest(Method.HEAD,fileurl).execute();
            //没有正常返回 无需执行其他
            if(response.getStatus()!= HttpStatus.HTTP_OK){
                return;
            }
            //文件类型校验
            String header = response.header("Content-type");
            if(StrUtil.isNotBlank(header)){
                final List<String> ALLOW_FORMAT_LIST= Arrays.asList("image/jpeg","image/jpg","image/png","image/webp");
                ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(header.toLowerCase()),ErrorCode.PARAMS_ERROR,"文件类型错误");
                String headeredLength = response.header("Content-Length");
                if(StrUtil.isNotBlank(headeredLength)){
                    try{
                        long Length = Long.parseLong(headeredLength);
                        final long ONE_M=1024*1024L;
                        ThrowUtils.throwIf(Length>2*ONE_M,ErrorCode.PARAMS_ERROR,"文件大小不能超过2M");
                    }catch (NumberFormatException e){
                        e.printStackTrace();
                    }
                }
            }

        }finally {
            response.close();
        }


    }

    /**
     * 图片文件检验
     * @param multipartFile
     */
    public void validPicture(MultipartFile multipartFile){
       // 检查文件是否为空
        ThrowUtils.throwIf(multipartFile==null, ErrorCode.PARAMS_ERROR,"文件不能为空");
        long fileSize = multipartFile.getSize();
        //检查文件大小
        final long ONE_M=1024*1024L;
        ThrowUtils.throwIf(fileSize>2*ONE_M,ErrorCode.PARAMS_ERROR,"文件大小不能超过2M");
        //检查文件后缀
        String suffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        final List<String> ALLOW_FORMAT_LIST= Arrays.asList("jpeg","jpg","png","webp");
        ThrowUtils.throwIf(ALLOW_FORMAT_LIST.contains(suffix),ErrorCode.PARAMS_ERROR,"文件类型错误");
    }

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
