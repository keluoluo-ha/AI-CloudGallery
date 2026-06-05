package com.hhk.picturebacked.service.impl;
import java.awt.*;
import java.io.IOException;
import java.util.*;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hhk.picturebacked.api.Aliyunimage.AliYunAiApi;
import com.hhk.picturebacked.constant.UserConstant;
import com.hhk.picturebacked.exception.BusinessException;
import com.hhk.picturebacked.exception.ErrorCode;
import com.hhk.picturebacked.exception.ThrowUtils;
import com.hhk.picturebacked.manager.CosManager;
import com.hhk.picturebacked.manager.FileManager;
import com.hhk.picturebacked.manager.upload.FilePictureUploadResouseImpl;
import com.hhk.picturebacked.manager.upload.PictureUploadResouse;
import com.hhk.picturebacked.manager.upload.URLPictureUploadResouseImpl;
import com.hhk.picturebacked.model.dto.picture.*;
import com.hhk.picturebacked.model.entity.Picture;
import com.hhk.picturebacked.model.entity.Space;
import com.hhk.picturebacked.model.entity.User;
import com.hhk.picturebacked.model.enums.PictureReviewStatusEnum;
import com.hhk.picturebacked.model.vo.PictureVO;
import com.hhk.picturebacked.model.vo.UserVO;
import com.hhk.picturebacked.service.PictureService;
import com.hhk.picturebacked.mapper.PictureMapper;
import com.hhk.picturebacked.service.SpaceService;
import com.hhk.picturebacked.service.UserService;
import com.hhk.picturebacked.config.CosClientConfig;
import com.hhk.picturebacked.utils.ColorSimilarUtils;
import com.hhk.picturebacked.utils.CosUrlUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.naming.SelectorContext.prefix;


/**
* @author 30674
* @description 针对表【picture(图片)】的数据库操作Service实现
* @createDate 2026-04-18 12:41:56
*/
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{

    @Resource
    public UserService userService;
    @Resource
    private FilePictureUploadResouseImpl filePictureUploadResouse;
    @Resource
    private URLPictureUploadResouseImpl urlPictureUploadResouse;
    @Resource
    private SpaceService spaceService;
    @Resource
    public TransactionTemplate transactionTemplate;
    @Resource
    public CosManager cosManager;
    @Resource
    private AliYunAiApi aliYunAiApi;
    @Resource
    private CosClientConfig cosClientConfig;


    @Override
    public void fillReviewParams(Picture picture,User loginUser){
        if(loginUser==null||loginUser.getId()<=0){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        //如果是管理员上传 自动过审
        if(Objects.equals(loginUser.getUserRole(), UserConstant.ADMIN_ROLE)){
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewerId(loginUser.getId());
            picture.setReviewTime(new Date());
        }else {
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }

    }

    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        String searchText = pictureUploadByBatchRequest.getSearchText();
        // 格式化数量
        Integer count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多 30 条");
        // 要抓取的地址
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isNull(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }
        Elements imgElementList = div.select("img.mimg");
        int uploadCount = 0;
        for (Element imgElement : imgElementList) {
            String fileUrl = imgElement.attr("src");
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空，已跳过: {}", fileUrl);
                continue;
            }
            // 处理图片上传地址，防止出现转义问题
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }
            // 上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
            if(StrUtil.isBlank(namePrefix)){
                namePrefix=searchText;
            }
            if(StrUtil.isNotBlank(namePrefix)){
            pictureUploadRequest.setPicName(namePrefix+(uploadCount+1));
            }
            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("图片上传成功, id = {}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败", e);
                continue;
            }
            if (uploadCount >= count) {
                break;
            }
        }
        return uploadCount;
    }


    @Override
    public PictureVO uploadPicture(Object inputResourse, PictureUploadRequest pictureUploadRequest, User loginUser) {
        // 判空
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        if (inputResourse == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件为空");
        }

        // 图片 ID（区分新增 / 更新）
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }

        // 旧图片（更新时用到）
        Picture oldPicture = null;
        if (pictureId != null) {
            oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            // 权限：必须是本人 OR 管理员（这里修复了！）
            boolean hasPermission = Objects.equals(oldPicture.getUserId(), loginUser.getId()) || userService.isAdmin(loginUser);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR, "无权限更新图片");
        }

        // 空间 ID 处理
        Long spaceId = null;
        if (pictureUploadRequest != null) {
            spaceId = pictureUploadRequest.getSpaceId();
        }

        // 更新图片时：如果没传 spaceId，使用旧图片的 spaceId
        if (pictureId != null && spaceId == null) {
            spaceId = oldPicture.getSpaceId() != null ? oldPicture.getSpaceId() : 0L;
        }

        // 空间权限 & 额度校验（只在【新增】或【切换空间】时校验）
        if (spaceId != null && spaceId != 0) {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");

            // 只有空间创建者能上传
            if (!loginUser.getId().equals(space.getUserId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间权限");
            }

            // 新增图片才校验额度，更新图片不校验（只替换大小）
            if (pictureId == null) {
                if (space.getTotalCount() >= space.getMaxCount()) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间条数不足");
                }
                if (space.getTotalSize() >= space.getMaxSize()) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间大小不足");
                }
            }
        }

        // 上传路径
        String uploadPathPrefix;
        if (spaceId == null || spaceId == 0) {
            uploadPathPrefix = String.format("public/%s", loginUser.getId());
        } else {
            uploadPathPrefix = String.format("space/%s", spaceId);
        }

        // 选择上传模板
        PictureUploadResouse picturetemplate = filePictureUploadResouse;
        if (inputResourse instanceof String) {
            picturetemplate = urlPictureUploadResouse;
        }

        // 执行上传
        UploadPictureResult uploadPictureResult = picturetemplate.uploadPicture(inputResourse, uploadPathPrefix);

        // 构造图片实体
        Picture picture = new Picture();
        picture.setId(pictureId);
        picture.setUserId(loginUser.getId());
        picture.setSpaceId(spaceId == null ? 0L : spaceId);
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());

        // 图片名称
        String picName = uploadPictureResult.getPicName();
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);

        // 图片信息
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setPicColor(uploadPictureResult.getPicColor());

        // 审核信息
        this.fillReviewParams(picture, loginUser);

        // 编辑时间
        if (pictureId != null) {
            picture.setEditTime(new Date());
        }

        // ====================== 核心修复：事务 + 额度精准计算 ======================
        Long finalSpaceId = spaceId == null ? 0L : spaceId;
        Picture finalOldPicture = oldPicture;

        transactionTemplate.execute(status -> {
            // 1. 保存或更新图片
            boolean saveOrUpdate = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(!saveOrUpdate, ErrorCode.OPERATION_ERROR, "图片保存失败");

            // 2. 只有归属空间才处理额度
            if (finalSpaceId == 0) {
                return picture;
            }

            if (finalOldPicture == null) {
                // ================ 新增图片：直接加 ================
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, finalSpaceId)
                        .setSql("totalSize = totalSize + " + picture.getPicSize())
                        .setSql("totalCount = totalCount + 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "空间额度更新失败");
            } else {
                // ================ 更新图片：先扣旧的，再加新的 ================
                long oldSize = finalOldPicture.getPicSize() == null ? 0 : finalOldPicture.getPicSize();
                long newSize = picture.getPicSize();

                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, finalSpaceId)
                        .setSql("totalSize = totalSize - " + oldSize + " + " + newSize)
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "空间额度更新失败");
            }
            return picture;
        });

        return PictureVO.objToVo(picture);
    }
    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        Long spaceId = picture.getSpaceId();
        if (spaceId == 0) {
            // 公共图库，仅本人或管理员可操作
            if (!picture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        } else {
            // 私有空间，仅空间管理员可操作
            if (!picture.getUserId().equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
    }



    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest request) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = request.getId();
        String name = request.getName();
        String introduction = request.getIntroduction();
        String category = request.getCategory();
        List<String> tags = request.getTags();
        Long picSize = request.getPicSize();
        Integer picWidth = request.getPicWidth();
        Integer picHeight = request.getPicHeight();
        Double picScale = request.getPicScale();
        String picFormat = request.getPicFormat();
        String searchText = request.getSearchText();
        Long userId = request.getUserId();
        String sortField = request.getSortField();
        String sortOrder = request.getSortOrder();
        //补充 审核数据
        Integer reviewStatus = request.getReviewStatus();
        String reviewMessage = request.getReviewMessage();
        Long spaceId = request.getSpaceId();
        Long reviewerId = request.getReviewerId();
        boolean nullSpaceId = request.isNullSpaceId();

        if (StrUtil.isNotBlank(searchText)) {
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText));
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.eq(nullSpaceId, "spaceId",0);
        Date startEditTime = request.getStartEditTime();
        Date endEditTime = request.getEndEditTime();
        queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "editTime", startEditTime);
        queryWrapper.lt(ObjUtil.isNotEmpty(endEditTime), "editTime", endEditTime);

        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.eq(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);


        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        // 对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVo(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }


    @Override
    public Page<PictureVO> getpictureVOList(Page<Picture>picturePage,HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if(CollUtil.isEmpty(pictureList)){
            return pictureVOPage;
        }
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        Set<Long> UserSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(UserSet).stream().collect(Collectors.groupingBy(User::getId));
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user=null;
            UserVO userVo=null;
            if(userIdUserListMap.containsKey(userId)){
                user=userIdUserListMap.get(userId).get(0);
                userVo = userService.getUserVo(user);
            }
            pictureVO.setUser(userVo);
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum enumByValue = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        if(id==null||enumByValue==null||PictureReviewStatusEnum.REVIEWING.equals(enumByValue)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //判断是否存在
        Picture Oldpicture = this.getById(id);
        ThrowUtils.throwIf(Oldpicture==null,ErrorCode.PARAMS_ERROR);
        //校验状态是否重复
        if(Objects.equals(pictureReviewRequest.getReviewStatus(), Oldpicture.getReviewStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "切勿重复操作");
        }
        //数据库操作
        Picture updatePicture = new Picture();
        BeanUtils.copyProperties(pictureReviewRequest,updatePicture);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        boolean res = this.updateById(updatePicture);
        ThrowUtils.throwIf(!res,ErrorCode.OPERATION_ERROR);

    }
    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        // 判断该图片是否被多条记录使用
        String pictureUrl = oldPicture.getUrl();
        long count = this.lambdaQuery()
                .eq(Picture::getUrl, pictureUrl)
                .count();
        // 有不止一条记录用到了该图片，不清理
        if (count > 1) {
            return;
        }
        String objectKey = CosUrlUtils.extractObjectKey(oldPicture.getUrl(), cosClientConfig.getHost());
        cosManager.deleteObject(objectKey);
        // 清理缩略图
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            String thumbnailKey = CosUrlUtils.extractObjectKey(thumbnailUrl, cosClientConfig.getHost());
            cosManager.deleteObject(thumbnailKey);
        }
    }

    @Override
    public void deletePicture(long pictureId, User loginUser) {
        ThrowUtils.throwIf(pictureId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 判断是否存在
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验权限
        checkPictureAuth(loginUser, oldPicture);

        // 开启事务
        transactionTemplate.execute(status -> {
            // 操作数据库
            boolean result = this.removeById(pictureId);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
            // 释放额度
            Long spaceId = oldPicture.getSpaceId();
            if (spaceId != 0) {
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, spaceId)
                        .setSql("totalSize = totalSize - " + oldPicture.getPicSize())
                        .setSql("totalCount = totalCount - 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            return true;
        });

        // 异步清理文件
        this.clearPictureFile(oldPicture);
    }

    @Override
    public void editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        //转换为实体picture类
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest,picture);
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        picture.setEditTime(new Date());
        //实体类校验
        this.validPicture(picture);
        //校验照片id是否为空
        Long pictureId = picture.getId();
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture==null,ErrorCode.NOT_FOUND_ERROR);
        //只有本人或者管理员可以修改
        if(!Objects.equals(oldPicture.getUserId(),loginUser.getId()) &&!userService.isAdmin(loginUser)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        //补充审核参数
        this.fillReviewParams(picture,loginUser);
        //数据库更新实体类
        boolean res = this.updateById(picture);
        ThrowUtils.throwIf(!res,ErrorCode.SYSTEM_ERROR);
    }

    @Override
    public List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser) {
        // 1. 校验参数
        ThrowUtils.throwIf(spaceId == null || StrUtil.isBlank(picColor), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 2. 校验空间权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        if (!loginUser.getId().equals(space.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间访问权限");
        }
        // 3. 查询该空间下所有图片（必须有主色调）
        List<Picture> pictureList = this.lambdaQuery()
                .eq(Picture::getSpaceId, spaceId)
                .isNotNull(Picture::getPicColor)
                .list();
        // 如果没有图片，直接返回空列表
        if (CollUtil.isEmpty(pictureList)) {
            return Collections.emptyList();
        }
        // 将目标颜色转为 Color 对象
        Color targetColor = Color.decode(picColor);
        // 4. 计算相似度并排序
        List<Picture> sortedPictures = pictureList.stream()
                .sorted(Comparator.comparingDouble(picture -> {
                    // 提取图片主色调
                    String hexColor = picture.getPicColor();
                    // 没有主色调的图片放到最后
                    if (StrUtil.isBlank(hexColor)) {
                        return Double.MAX_VALUE;
                    }
                    Color pictureColor = Color.decode(hexColor);
                    // 越大越相似
                    return -ColorSimilarUtils.calculateSimilarity(targetColor, pictureColor);
                }))
                // 取前 12 个
                .limit(12)
                .collect(Collectors.toList());

        // 转换为 PictureVO
        return sortedPictures.stream()
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser) {
        List<Long> pictureIdList = pictureEditByBatchRequest.getPictureIdList();
        Long spaceId = pictureEditByBatchRequest.getSpaceId();
        String category = pictureEditByBatchRequest.getCategory();
        List<String> tags = pictureEditByBatchRequest.getTags();
        // 批量重命名
        String nameRule = pictureEditByBatchRequest.getNameRule();


        //校验参数
        ThrowUtils.throwIf(spaceId==null||CollUtil.isEmpty(pictureIdList),ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser==null,ErrorCode.NO_AUTH_ERROR);
        //校验权限
        Space spaceServiceById = spaceService.getById(spaceId);
        ThrowUtils.throwIf(spaceServiceById==null,ErrorCode.NOT_FOUND_ERROR);
        if(loginUser.getId().equals(spaceServiceById.getUserId())){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"没有空间访问权限");
        }
        //查询指定图片 仅选择需要的字段
        List<Picture> list = this.lambdaQuery()
                .select(Picture::getId, Picture::getSpaceId)
                .eq(Picture::getId, pictureIdList)
                .in(Picture::getId, pictureIdList)
                .list();
        fillPictureWithNameRule(list, nameRule);
        //更新分类和标签
        if(list.isEmpty()){
            return;
        }
        //批量更新
        list.forEach(picture -> {
            if(StrUtil.isNotBlank(category)){
                picture.setCategory(category);
            }
            if(CollUtil.isNotEmpty(tags)){
                picture.setTags(JSONUtil.toJsonStr(tags));
            }

        });
        boolean res = this.updateById((Picture) pictureIdList);
        ThrowUtils.throwIf(!res,ErrorCode.OPERATION_ERROR);
    }

    @Override
    public CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser) {
        //获取图片
        Long pictureId = createPictureOutPaintingTaskRequest.getPictureId();
        Picture picture = Optional.ofNullable(this.getById(pictureId)).orElseThrow(() -> new BusinessException(ErrorCode.NO_AUTH_ERROR));

        //权限校验
        checkPictureAuth(loginUser,picture);
        //请求构造参数
        CreateOutPaintingTaskRequest TaskRequest = new CreateOutPaintingTaskRequest();
        CreateOutPaintingTaskRequest.Input input=new CreateOutPaintingTaskRequest.Input();
        input.setImageUrl(picture.getUrl());
        TaskRequest.setInput(input);
        BeanUtils.copyProperties(createPictureOutPaintingTaskRequest,TaskRequest);
        return aliYunAiApi.createOutPaintingTask(TaskRequest);
    }

    /**
     * nameRule 格式：图片{序号}
     *
     * @param pictureList
     * @param nameRule
     */
    private void fillPictureWithNameRule(List<Picture> pictureList, String nameRule) {
        if (CollUtil.isEmpty(pictureList) || StrUtil.isBlank(nameRule)) {
            return;
        }
        long count = 1;
        try {
            for (Picture picture : pictureList) {
                String pictureName = nameRule.replaceAll("\\{序号}", String.valueOf(count++));
                picture.setName(pictureName);
            }
        } catch (Exception e) {
            log.error("名称解析错误", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "名称解析错误");
        }
    }


}


