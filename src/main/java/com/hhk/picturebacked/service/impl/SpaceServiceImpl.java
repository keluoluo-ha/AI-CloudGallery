package com.hhk.picturebacked.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hhk.picturebacked.exception.BusinessException;
import com.hhk.picturebacked.exception.ErrorCode;
import com.hhk.picturebacked.exception.ThrowUtils;
import com.hhk.picturebacked.manager.sharding.DynamicShardingManager;
import com.hhk.picturebacked.mapper.SpaceMapper;
import com.hhk.picturebacked.model.dto.space.SpaceAddRequest;
import com.hhk.picturebacked.model.dto.space.SpaceQueryRequest;
import com.hhk.picturebacked.model.entity.Space;
import com.hhk.picturebacked.model.entity.SpaceUser;
import com.hhk.picturebacked.model.entity.User;
import com.hhk.picturebacked.model.enums.SpaceLevelEnum;
import com.hhk.picturebacked.model.enums.SpaceRoleEnum;
import com.hhk.picturebacked.model.enums.SpaceTypeEnum;
import com.hhk.picturebacked.model.vo.SpaceVO;
import com.hhk.picturebacked.model.vo.UserVO;
import com.hhk.picturebacked.service.SpaceService;
import com.hhk.picturebacked.service.SpaceUserService;
import com.hhk.picturebacked.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
* @author 30674
* @description 针对表【space(空间)】的数据库操作Service实现
* @createDate 2026-04-27 15:45:53
*/
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceService {

      @Lazy
      @Resource
      public SpaceUserService spaceUserService;

        @Resource
        public UserService userService;

        @Resource
        public TransactionTemplate transactionTemplate;

        @Resource
        @Lazy
        public DynamicShardingManager dynamicShardingManager;


    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        if (spaceQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        Integer spaceType = spaceQueryRequest.getSpaceType();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();
        // 拼接查询条件
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceType), "spaceType", spaceType);
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }


    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        // 对象转封装类
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        // 关联查询用户信息
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVo(user);
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }


    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        List<Space> spaceList = spacePage.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }
        // 对象列表 => 封装对象列表
        List<SpaceVO> spaceVOList = spaceList.stream()
                .map(SpaceVO::objToVo)
                .collect(Collectors.toList());
        // 1. 关联查询用户信息
        // 1,2,3,4
        Set<Long> userIdSet = spaceList.stream().map(Space::getUserId).collect(Collectors.toSet());
        // 1 => user1, 2 => user2
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceVO.setUser(userService.getUserVo(user));
        });
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    @Override
    public long addSpace(SpaceAddRequest addRequest, User loginUser) {
        //进行实体类和dto进行转换
        Space space = new Space();
        BeanUtils.copyProperties(addRequest,space);
        if(StrUtil.isBlank(addRequest.getSpaceName())){
            space.setSpaceName("默认空间");
        }
        if(addRequest.getSpaceLevel()==null){
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        if(addRequest.getSpaceType()==null){
            addRequest.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        }
        //数据填充
        this.fillSpaceBySpaceLevel(space);
        //数据检验
        this.validSpace(space,false);
        //权限校验
        Long userId = loginUser.getId();
       if(SpaceLevelEnum.COMMON.getValue()!=addRequest.getSpaceLevel()&&!userService.isAdmin(loginUser)){
           throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"无权限创建指定级别的空间");
       }
       //本地锁防止一个用户创建多个空间
        String lock = String.valueOf(userId).intern();
       synchronized (lock){
           Long newSpaceid = transactionTemplate.execute(status -> {
               boolean exists = this.lambdaQuery()
                       .eq(Space::getUserId, userId)
                       .eq(Space::getSpaceType,addRequest.getSpaceType())
                       .exists();
               ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "每个用户仅能有一个私有空间");
               space.setUserId(userId);
               boolean save = this.save(space);
               ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR);
               //如果是团队空间 创建space_user(空间用户关联）
               if(SpaceTypeEnum.TEAM.getValue()==addRequest.getSpaceType()){
                   SpaceUser spaceUser = new SpaceUser();
                   spaceUser.setSpaceId(space.getId());
                   spaceUser.setUserId(userId);
                   spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
                   boolean result = spaceUserService.save(spaceUser);
                   ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "创建团队成员记录失败");
               }
                dynamicShardingManager.createSpacePictureTable(space);
               return space.getId();





           });
           return Optional.ofNullable(newSpaceid).orElse(-1L);
       }


    }

    @Override
    public void validSpace(Space space, boolean add) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        Integer spaceType = space.getSpaceType();
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(spaceType);

        // 创建时校验
        if (add) {
            if (StrUtil.isBlank(spaceName)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            }
            if (spaceLevel == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不能为空");
            }
            if (spaceType == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类别不能为空");
            }
        }
        // 修改数据时，空间名称进行校验
        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");
        }
        // 修改数据时，空间级别进行校验
        if (spaceLevel != null && spaceLevelEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不存在");
        }
        if(spaceType!=null&&spaceTypeEnum==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间类型不存在");
        }

    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        // 根据空间级别，自动填充限额
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if (spaceLevelEnum != null) {
            long maxSize = spaceLevelEnum.getMaxSize();
            if (space.getMaxSize() == null) {
                space.setMaxSize(maxSize);
            }
            long maxCount = spaceLevelEnum.getMaxCount();
            if (space.getMaxCount() == null) {
                space.setMaxCount(maxCount);
            }
        }
    }

    @Override
    public void checkSpaceAuth(User loginUser, Space space) {
        if(space.getUserId()==null|| !space.getUserId().equals(loginUser.getId()))
        {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }


}




