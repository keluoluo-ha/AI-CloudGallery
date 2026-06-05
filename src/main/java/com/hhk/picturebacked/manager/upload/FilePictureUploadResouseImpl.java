package com.hhk.picturebacked.manager.upload;

import cn.hutool.core.io.FileUtil;
import com.hhk.picturebacked.exception.ErrorCode;
import com.hhk.picturebacked.exception.ThrowUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;


@Service
public class FilePictureUploadResouseImpl extends PictureUploadResouse {
    @Override
    public String getOriginalFilename(Object inputResourse) {
        MultipartFile multipartFile = (MultipartFile) inputResourse;
        return multipartFile.getOriginalFilename();
    }

    @Override
    public void transferToFile(Object inputResourse, File file) throws IOException {
        MultipartFile multipartFile = (MultipartFile) inputResourse;
        multipartFile.transferTo(file);

    }

    @Override
    public void validPicture(Object inputResourse) {
        MultipartFile multipartFile = (MultipartFile) inputResourse;
        // 检查文件是否为空
        ThrowUtils.throwIf(multipartFile==null, ErrorCode.PARAMS_ERROR,"文件不能为空");
        long fileSize = multipartFile.getSize();
        //检查文件大小
        final long ONE_M=1024*1024L;
        ThrowUtils.throwIf(fileSize>2*ONE_M,ErrorCode.PARAMS_ERROR,"文件大小不能超过2M");
        //检查文件后缀
        String suffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        final List<String> ALLOW_FORMAT_LIST= Arrays.asList("jpeg","jpg","png","webp");
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(suffix),ErrorCode.PARAMS_ERROR,"文件类型错误");
    }

}
