package com.hhk.picturebacked.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hhk.picturebacked.commom.DeleteRequest;
import com.hhk.picturebacked.model.dto.picture.*;
import com.hhk.picturebacked.model.dto.user.UserQueryRequest;
import com.hhk.picturebacked.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hhk.picturebacked.model.entity.User;
import com.hhk.picturebacked.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 30674
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2026-04-18 12:41:56
*/
public interface PictureService extends IService<Picture> {

    /**
     * 上传图片
     *
     * @param inputResourse
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    PictureVO uploadPicture(Object inputResourse,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);


    /**
     * 检查是否有权限
     * @param loginUser
     * @param picture
     */
     void checkPictureAuth(User loginUser, Picture picture);

    /**
     * AI 产物上传：强制走待审核，不允许自动过审
     *
     * @param imageUrl             AI 返回的图片地址
     * @param pictureUploadRequest 上传参数
     * @param loginUser            提交用户
     * @return 落库后的图片
     */
    Picture uploadAiResultPicture(String imageUrl, PictureUploadRequest pictureUploadRequest, User loginUser);

    /**
     * 查询wrapper
     * @param request
     * @return
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest request);



    /**
     *
     *封装单条图片
     * @param picture
     * @param request
     * @return
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     *封装图片列表
     * @param picturePage
     * @param request
     * @return
     */
    Page<PictureVO> getpictureVOList(Page<Picture>picturePage, HttpServletRequest request);

    /**
     * 图片数据校验
     * @param picture
     */
    void validPicture(Picture picture);


    /**
     * 图片审核
     *
     * @param pictureReviewRequest
     * @param loginUser
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);


    /**
     * 自动填入审核数据（管理员自动成功）
     * @param picture
     * @param loginUser
     */
    void fillReviewParams(Picture picture,User loginUser);


    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return 成功创建的图片数
     */
    Integer uploadPictureByBatch(
            PictureUploadByBatchRequest pictureUploadByBatchRequest,
            User loginUser
    );

    /**
     * 图片文件清除（cos）
     * @param oldPicture
     */
    void clearPictureFile(Picture oldPicture);

    /**
     * 图片删除
     * @param pictureId
     * @param loginUser
     */
     void deletePicture(long pictureId, User loginUser);

    /**
     * 用户修改图片
     * @param pictureEditRequest
     * @param loginUser
     */
     void editPicture(PictureEditRequest pictureEditRequest,User loginUser);

    /**
     * 按颜色查询
     * @param spaceId
     * @param picColor
     * @param loginUser
     * @return
     */
    List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser);

    /**
     * 批量修改图片
     * @param pictureEditByBatchRequest
     * @param loginUser
     */
    void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest,User loginUser);

    /**
     * ai扩图扩展
     * @param createPictureOutPaintingTaskRequest
     * @param loginUser
     * @return
     */
    CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser);
}
