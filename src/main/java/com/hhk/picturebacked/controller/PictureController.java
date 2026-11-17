package com.hhk.picturebacked.controller;



import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hhk.picturebacked.annotation.AuthCheck;
import com.hhk.picturebacked.api.Aliyunimage.AliYunAiApi;
import com.hhk.picturebacked.auth.SpaceUserAuthManager;
import com.hhk.picturebacked.auth.StpKit;
import com.hhk.picturebacked.auth.annotation.SaSpaceCheckPermission;
import com.hhk.picturebacked.auth.model.SpaceUserPermissionConstant;
import com.hhk.picturebacked.commom.BaseResponse;
import com.hhk.picturebacked.commom.DeleteRequest;
import com.hhk.picturebacked.commom.ResultUtils;
import com.hhk.picturebacked.constant.UserConstant;
import com.hhk.picturebacked.exception.BusinessException;
import com.hhk.picturebacked.exception.ErrorCode;
import com.hhk.picturebacked.exception.ThrowUtils;
import com.hhk.picturebacked.config.CosClientConfig;
import com.hhk.picturebacked.manager.CosManager;
import com.hhk.picturebacked.utils.CosUrlUtils;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;

import com.hhk.picturebacked.model.dto.picture.*;
import com.hhk.picturebacked.model.entity.Picture;
import com.hhk.picturebacked.model.entity.Space;
import com.hhk.picturebacked.model.entity.User;
import com.hhk.picturebacked.model.enums.PictureReviewStatusEnum;
import com.hhk.picturebacked.model.vo.PictureVO;
import com.hhk.picturebacked.model.vo.PictureViewRankVO;
import com.hhk.picturebacked.service.PictureService;
import com.hhk.picturebacked.service.PictureViewService;
import com.hhk.picturebacked.service.SpaceService;
import com.hhk.picturebacked.service.UserService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/picture")
public class PictureController {

    @Resource
    public UserService userService;
    @Resource
    public PictureService pictureService;
    @Resource
    public CosManager cosManager;
    @Resource
    private CosClientConfig cosClientConfig;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    public SpaceService spaceService;
    @Resource
    public SpaceUserAuthManager spaceUserAuthManager;

    @Resource
    private AliYunAiApi aliYunAiApi;
    @Resource
    private PictureViewService pictureViewService;
    //构造本地缓存
    private final Cache<String, String> LOCAL_CACHE =
            Caffeine.newBuilder().initialCapacity(1024)
                    .maximumSize(10000L)
                    // 缓存 5 分钟移除
                    .expireAfterWrite(5L, TimeUnit.MINUTES)
                    .build();



    /**
     * 管理员和用户上传图片
     * @param multipartFile
     * @param pictureUploadRequest
     * @param request
     * @return
     */
    @PostMapping("/upload")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPicture(@RequestPart("file") MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, HttpServletRequest request){
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }
    /**
     * 通过 URL 上传图片（可重新上传）
     */
    @PostMapping("/upload/url")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPictureByUrl(
            @RequestBody PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        String fileUrl = pictureUploadRequest.getUrl();
        PictureVO pictureVO = pictureService.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }


    /**
     * 删除图片
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_DELETE)
    public BaseResponse<Boolean> deleteById(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        Long id = deleteRequest.getId();
        pictureService.deletePicture(id, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 管理员更新图片
     * @param pictureUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest,HttpServletRequest request){
        if(pictureUpdateRequest==null||pictureUpdateRequest.getId()<=0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest,picture);
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        pictureService.validPicture(picture);
        Long pictureId = picture.getId();
        User loginUser = userService.getLoginUser(request);
        if(pictureId!=null){
            Picture OldPicture = pictureService.getById(pictureId);
            //补充审核参数
            ThrowUtils.throwIf(OldPicture==null,ErrorCode.NOT_FOUND_ERROR);
            //只有本人或者管理员可以编辑
            if(!OldPicture.getUserId().equals(loginUser.getId())&&!userService.isAdmin(loginUser)){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }

        pictureService.fillReviewParams(picture,loginUser);
        //操作数据库
        boolean res = pictureService.updateById(picture);
        ThrowUtils.throwIf(!res,ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(res);

    }


    /**
     * 管理员获取图片
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(long id,HttpServletRequest request){
        ThrowUtils.throwIf(id<=0,ErrorCode.PARAMS_ERROR);
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture==null,ErrorCode.NOT_FOUND_ERROR);
        Long spaceId = picture.getSpaceId();
        if(spaceId!=null){
            User loginUser = userService.getLoginUser(request);
            pictureService.checkPictureAuth(loginUser,picture);
        }
        return ResultUtils.success(picture);
    }

    /**
     * 根据 id 获取图片（封装类）
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 空间的图片，需要校验权限
        Space space = null;
        Long spaceId = picture.getSpaceId();
        if (spaceId != 0) {
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
            space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        // 获取权限列表
        User loginUser = userService.getLoginUser(request);
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        PictureVO pictureVO = pictureService.getPictureVO(picture, request);
        pictureVO.setPermissionList(permissionList);
        // 获取封装类
        return ResultUtils.success(pictureVO);
    }

    /**
     * 上报图片浏览量
     * 流程：Redis SETNX 防刷（1 小时内同一用户同一图片只计一次）-> 发一条 MQ 消息 -> 立即返回
     * 这里不更新数据库，浏览量由消费者计数到 Redis，再由定时任务批量刷回 MySQL
     */
    @PostMapping("/view/{pictureId}")
    public BaseResponse<Boolean> reportPictureView(@PathVariable long pictureId, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureId <= 0, ErrorCode.PARAMS_ERROR);
        // 取当前登录用户；未登录时用 IP 做兜底标识，同样能起到防刷作用
        Long userId = 0L;
        try {
            User loginUser = userService.getLoginUser(request);
            if (loginUser != null && loginUser.getId() != null) {
                userId = loginUser.getId();
            }
        } catch (Exception e) {
            // 未登录：退化成 IP 维度的防刷
            userId = Math.abs((long) String.valueOf(request.getRemoteAddr()).hashCode());
        }
        pictureViewService.reportView(pictureId, userId);
        return ResultUtils.success(true);
    }

    /**
     * 周浏览量 TOP10（暂时只提供接口，前端暂未接入）
     * 直接读 Redis 计数 key 排序返回，不做多日合并、不缓存、不做冷启动降级
     */
    @GetMapping("/rank/weekly")
    public BaseResponse<List<PictureViewRankVO>> getWeeklyViewRank() {
        List<PictureViewRankVO> rank = pictureViewService.getWeeklyRank(10);
        return ResultUtils.success(rank);
    }

    /**
     * 获取图片文件流（同源代理，供前端裁切组件使用，避免 COS 跨域）
     */
    @GetMapping("/image/{id}")
    public void getPictureImage(@PathVariable long id, HttpServletResponse response) throws IOException {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        Long spaceId = picture.getSpaceId();
        if (spaceId != null && spaceId != 0) {
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
        }
        String objectKey = CosUrlUtils.extractObjectKey(picture.getUrl(), cosClientConfig.getHost());
        COSObjectInputStream cosObjectInput = null;
        try {
            COSObject cosObject = cosManager.downloadCOS(objectKey);
            cosObjectInput = cosObject.getObjectContent();
            String contentType = cosObject.getObjectMetadata().getContentType();
            response.setContentType(StrUtil.isNotBlank(contentType) ? contentType : "image/jpeg");
            response.setHeader("Cache-Control", "max-age=3600");
            IOUtils.copy(cosObjectInput, response.getOutputStream());
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("获取图片文件失败, id = {}", id, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取图片失败");
        } finally {
            if (cosObjectInput != null) {
                cosObjectInput.close();
            }
        }
    }

    /**
     * 管理员获取图片列表
     * @param pictureQueryRequest
     * @return
     */
    @PostMapping("list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest){
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        Page<Picture> picturePage=pictureService.page(new Page<>(current,pageSize),pictureService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(picturePage);

    }


    /**
     * 获取图片列表
     * @param pictureQueryRequest
     * @param request
     * @return
     */
   @PostMapping("/list/page/vo")
    public BaseResponse< Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request){
       ThrowUtils.throwIf(pictureQueryRequest==null,ErrorCode.PARAMS_ERROR);
       int current = pictureQueryRequest.getCurrent();
       int pageSize = pictureQueryRequest.getPageSize();
       //限制爬虫
       ThrowUtils.throwIf(pageSize>20,ErrorCode.PARAMS_ERROR);
       // 公共图库才只展示已过审图片；空间内查询展示该空间全部图片
       Long spaceId = pictureQueryRequest.getSpaceId();
       if (spaceId == null || spaceId == 0) {
           pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
       }
       Page<Picture> page = pictureService.page(new Page<>(current, pageSize), pictureService.getQueryWrapper(pictureQueryRequest));

       Page<PictureVO> pictureVOPage = pictureService.getpictureVOList(page, request);
       return ResultUtils.success(pictureVOPage);
   }

    /**
     * redis缓存
     * @param pictureQueryRequest
     * @param request
     * @return
     */
    @GetMapping("/list/page/vo/cache")
    public BaseResponse< Page<PictureVO>> listPictureVOByPageCache( PictureQueryRequest pictureQueryRequest,HttpServletRequest request){
        ThrowUtils.throwIf(pictureQueryRequest==null,ErrorCode.PARAMS_ERROR);
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        //限制爬虫
        ThrowUtils.throwIf(pageSize>20,ErrorCode.PARAMS_ERROR);

        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        //构建缓存key
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashkey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String rediskey = "picture:listPictureVOByPageCache" + hashkey;
        //从redis中查询缓存
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        String cacheValue = ops.get(rediskey);
        //命中缓存 返回结果
        if(cacheValue!=null){
            Page<PictureVO> bean = JSONUtil.toBean(cacheValue, Page.class);
            return ResultUtils.success(bean);
        }
        Page<Picture> page = pictureService.page(new Page<>(current, pageSize), pictureService.getQueryWrapper(pictureQueryRequest));
        //如果没有 查询数据库普通用户只能看到已经过审的数据
        Page<PictureVO> pictureVOPage = pictureService.getpictureVOList(page, request);
        //5-10分钟随机过期，防止雪崩；
        String cachevalue = JSONUtil.toJsonStr(pictureVOPage);
        int cacheExprieTime=300+ RandomUtil.randomInt(0,300);
        ops.set(rediskey,cachevalue,cacheExprieTime, TimeUnit.SECONDS);
        return ResultUtils.success(pictureVOPage);
    }

    /**
     * 多级缓存
     * @param pictureQueryRequest
     * @param request
     * @return
     */
    @GetMapping("/picture/list/page/vo/caffeinecache")
    public BaseResponse< Page<PictureVO>> listPictureVOByPagecaffeinecache( PictureQueryRequest pictureQueryRequest,HttpServletRequest request){
        ThrowUtils.throwIf(pictureQueryRequest==null,ErrorCode.PARAMS_ERROR);
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        //限制爬虫
        ThrowUtils.throwIf(pageSize>20,ErrorCode.PARAMS_ERROR);

        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        //构建缓存key
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashkey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String caffinekey = "listPictureVOByPageCache" + hashkey;
        String rediskey = "picture:listPictureVOByPageCache" + hashkey;
        //从本地中查询缓存
        String cacheValue = LOCAL_CACHE.getIfPresent(caffinekey);

        //命中缓存 返回结果
        if(cacheValue!=null){
            Page<PictureVO> bean = JSONUtil.toBean(cacheValue, Page.class);
            return ResultUtils.success(bean);
        }
        // 2. 查询分布式缓存（Redis）
        ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();
        cacheValue = valueOps.get(rediskey);
        if (cacheValue != null) {
            // 如果命中 Redis，存入本地缓存并返回
            LOCAL_CACHE.put(caffinekey, cacheValue);
            Page<PictureVO> cachedPage = JSONUtil.toBean(cacheValue, Page.class);
            return ResultUtils.success(cachedPage);
        }

        Page<Picture> page = pictureService.page(new Page<>(current, pageSize), pictureService.getQueryWrapper(pictureQueryRequest));
        //如果没有 查询数据库普通用户只能看到已经过审的数据
        Page<PictureVO> pictureVOPage = pictureService.getpictureVOList(page, request);

        //存入redis缓存 5-10分钟随机过期，防止雪崩；
        String cachevalue = JSONUtil.toJsonStr(pictureVOPage);
        int cacheExprieTime=300+ RandomUtil.randomInt(0,300);
        valueOps.set(rediskey,cachevalue,cacheExprieTime, TimeUnit.SECONDS);


        return ResultUtils.success(pictureVOPage);
    }





    /**
     * 用户编辑图片
     * @param pictureEditRequest
     * @param request
     * @return
     */
   @PostMapping("/edit")
   @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
   public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest,HttpServletRequest request){
        if(pictureEditRequest==null||pictureEditRequest.getId()<=0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
       User loginUser = userService.getLoginUser(request);
       pictureService.editPicture(pictureEditRequest,loginUser);
       return ResultUtils.success(true);

   }


    /**
     * 获取图片标签
     * @return
     */
    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }


    /**
     * 上传图片审核接口（仅管理员）
     * @param pictureReviewRequest
     * @param request
     * @return
     */
    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> doPicture(@RequestBody PictureReviewRequest pictureReviewRequest,HttpServletRequest request){
       ThrowUtils.throwIf(pictureReviewRequest==null,ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.doPictureReview(pictureReviewRequest,loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 批量抓取图片
     * @param pictureUploadByBatchRequest
     * @param request
     * @return
     */
    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(
            @RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,
            HttpServletRequest request)
    {
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        int uploadCount = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        return ResultUtils.success(uploadCount);
    }

    /**
     * 按照颜色搜索
     * @param searchPictureByColorRequest
     * @param request
     * @return
     */
    @PostMapping("/search/color")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<List<PictureVO>> searchPictureByColor(@RequestBody SearchPictureByColorRequest searchPictureByColorRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(searchPictureByColorRequest == null, ErrorCode.PARAMS_ERROR);
        String picColor = searchPictureByColorRequest.getPicColor();
        Long spaceId = searchPictureByColorRequest.getSpaceId();
        User loginUser = userService.getLoginUser(request);
        List<PictureVO> result = pictureService.searchPictureByColor(spaceId, picColor, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 批量编辑图片
     * @param pictureEditByBatchRequest
     * @param request
     * @return
     */
    @PostMapping("/edit/batch")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPictureByBatch(@RequestBody PictureEditByBatchRequest pictureEditByBatchRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureEditByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.editPictureByBatch(pictureEditByBatchRequest, loginUser);
        return ResultUtils.success(true);
    }

//    /**
//     * 以图搜图
//     */
//    @PostMapping("/search/picture")
//    public BaseResponse<List<ImageSearchResult>> searchPictureByPicture(@RequestBody SearchPictureByPictureRequest searchPictureByPictureRequest) {
//        ThrowUtils.throwIf(searchPictureByPictureRequest == null, ErrorCode.PARAMS_ERROR);
//        Long pictureId = searchPictureByPictureRequest.getPictureId();
//        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
//        Picture picture = pictureService.getById(pictureId);
//        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
//        List<ImageSearchResult> resultList = ImageSearchApiFacade.searchImage(picture.getUrl());
//        return ResultUtils.success(resultList);
//    }


    /**
     * 创建 AI 扩图任务
     */
    @PostMapping("/out_painting/create_task")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<CreateOutPaintingTaskResponse> createPictureOutPaintingTask(
            @RequestBody CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest,
            HttpServletRequest request) {
        if (createPictureOutPaintingTaskRequest == null || createPictureOutPaintingTaskRequest.getPictureId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        CreateOutPaintingTaskResponse response = pictureService.createPictureOutPaintingTask(createPictureOutPaintingTaskRequest, loginUser);
        return ResultUtils.success(response);
    }

    /**
     * 查询 AI 扩图任务
     */
    @GetMapping("/out_painting/get_task")
    public BaseResponse<GetOutPaintingTaskResponse> getPictureOutPaintingTask(String taskId) {
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR);
        GetOutPaintingTaskResponse task = aliYunAiApi.getOutPaintingTask(taskId);
        return ResultUtils.success(task);
    }




}
