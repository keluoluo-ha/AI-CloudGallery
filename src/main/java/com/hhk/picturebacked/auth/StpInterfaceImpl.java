package com.hhk.picturebacked.auth;

import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.json.JSONUtil;
import com.hhk.picturebacked.auth.model.SpaceUserPermissionConstant;
import com.hhk.picturebacked.constant.UserConstant;
import com.hhk.picturebacked.exception.BusinessException;
import com.hhk.picturebacked.exception.ErrorCode;
import com.hhk.picturebacked.model.entity.Picture;
import com.hhk.picturebacked.model.entity.Space;
import com.hhk.picturebacked.model.entity.SpaceUser;
import com.hhk.picturebacked.model.entity.User;
import com.hhk.picturebacked.model.enums.SpaceRoleEnum;
import com.hhk.picturebacked.model.enums.SpaceTypeEnum;
import com.hhk.picturebacked.service.PictureService;
import com.hhk.picturebacked.service.SpaceService;
import com.hhk.picturebacked.service.SpaceUserService;
import com.hhk.picturebacked.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 自定义权限加载接口实现类
 */
@Component    // 保证此类被 SpringBoot 扫描，完成 Sa-Token 的自定义权限验证扩展
public class StpInterfaceImpl implements StpInterface {

    @Resource
    public PictureService pictureService;
    @Resource
    public UserService userService;

    @Resource

    public SpaceService spaceService;

    @Resource
    @Lazy
    public SpaceUserService spaceUserService;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private SpaceUserAuthManager spaceUserAuthManager;

    /**
     * 返回账号的权限列表
     * @param loginType
     * @param loginId
     * @return
     */
    @Override
    public List<String> getPermissionList(Object loginId,String loginType){
        //判断loginType，只对space类型校验
        if(!loginType.equals(StpKit.SPACE_TYPE)){
            return new ArrayList<>();
        }
        //管理员权限列表，表示权限通过
        List<String> ADMIN_PERMISSION = spaceUserAuthManager.getPermissionByRole(SpaceRoleEnum.ADMIN.getValue());
        //获取送来请求上下文spaceUserAuthContext
        SpaceUserAuthContext authContext = getAuthContextByRequest();
        //表示查询公共图库 返回所有权限
        if(isAllFieldsNull(authContext)){
            return ADMIN_PERMISSION;
        }
        //获取到用户
        User user = (User) StpKit.SPACE.getSessionByLoginId(loginId).get(UserConstant.USER_LOGIN_STATE);
        //如果用户未登录 返回为空
        if(user==null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        //如果是系统管理员 返回所用权限
        if(userService.isAdmin(user)){
            return ADMIN_PERMISSION;
        }

        Long userId = user.getId();
        //优先从上下文获取SpaceUser
        SpaceUser spaceUser = authContext.getSpaceUser();
        //如果获取到了spaceUser 就说明是团队空间 只需要返回这个账户关于这个团队空间的权限
        if(spaceUser!=null){
            return spaceUserAuthManager.getPermissionByRole(spaceUser.getSpaceRole());
        }
        //如果有spaceId 说明是团队空间
        Long spaceUserId = authContext.getSpaceUserId();
        if(spaceUserId!=null)
        {
            spaceUser = spaceUserService.getById(spaceUserId);
            if(spaceUser==null){
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"未找到空间用户信息");
            }
            SpaceUser loginSpaceUser = spaceUserService.lambdaQuery().eq(SpaceUser::getSpaceId, spaceUser.getSpaceId())
                    .eq(SpaceUser::getUserId, userId)
                    .one();
            if(loginSpaceUser==null) return new ArrayList<>();
            return spaceUserAuthManager.getPermissionByRole(spaceUser.getSpaceRole());
        }
        //如果没有spacUserId 尝试通过pictureId或者spaceId获取对象
        Long spaceId = authContext.getSpaceId();
        if(spaceId==null) {
            //如果没有spaceId 通过pictureId尝试获取
            Long pictureId = authContext.getPictureId();
            if(pictureId==null){
                return ADMIN_PERMISSION;
            }
            Picture picture = pictureService.lambdaQuery()
                    .eq(Picture::getId,pictureId)
                    .one();
            if(picture==null){
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"未找到图片信息");
            }
            spaceId = picture.getSpaceId();
            //就是公共图库 只有本人或者管理员可以操作
            if(spaceId==0){
                System.out.println("picture.getUserId()"+picture.getUserId());
                System.out.println("userId"+userId);
                if(picture.getUserId().equals(userId)||userService.isAdmin(user)){
                    return ADMIN_PERMISSION;
                }
                else {
                    return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);
                }
            }


        }
        //如果有空间
        Space space = spaceService.getById(spaceId);
        if(space==null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"未找到空间信息");
        }
        //如果是私有空间
        if(space.getSpaceType()== SpaceTypeEnum.PRIVATE.getValue()){
            if(space.getUserId().equals(userId)||userService.isAdmin(user)){
                return ADMIN_PERMISSION;
            }else {
                return new ArrayList<>();
            }
            //如果是团队空间
        }else {
            spaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceId)
                    .eq(SpaceUser::getUserId, userId)
                    .one();
            if(spaceUser==null){
                return new ArrayList<>();
            }
            return spaceUserAuthManager.getPermissionByRole(spaceUser.getSpaceRole());
        }


    }

    /**
     * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 本 list 仅做模拟，实际项目中要根据具体业务逻辑来查询角色
       return new ArrayList<>();
    }

    /**
     * 从请求中获取上下文对象
     * @return
     */
    public SpaceUserAuthContext getAuthContextByRequest(){
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String contentType = request.getHeader(Header.CONTENT_TYPE.getValue());
        SpaceUserAuthContext spaceUserAuthContext;
        //兼容get和post操作
        if(ContentType.JSON.getValue().equals(contentType)){
            // 如果是 JSON 请求（POST 常用）
            String body = ServletUtil.getBody(request);
            //多出来的字段会自动忽略
            spaceUserAuthContext = JSONUtil.toBean(body, SpaceUserAuthContext.class);
        }else {
            // 如果是表单/GET 请求
            // 你在处理浏览器发来的请求 → 用 ServletUtil
            //你要调用别人的接口 / 网站 → 用 HttpUtil
            //总结
            //ServletUtil：处理浏览器 → 你的服务器的请求（内部 Web 工具）
            //HttpUtil：处理你的程序 → 别的服务器的请求（通用 HTTP 工具）
            //小白记住：处理页面请求用 ServletUtil，调外部接口用 HttpUtil
            Map<String, String> paramMap = ServletUtil.getParamMap(request);
            spaceUserAuthContext= BeanUtil.toBean(paramMap, SpaceUserAuthContext.class);
        }
        Long id = spaceUserAuthContext.getId();
        if (ObjUtil.isNotNull(id)) {
            String requestUri = request.getRequestURI();
            String partUri = requestUri.replace(contextPath + "/", "");
            String moduleName = StrUtil.subBefore(partUri, "/", false);
            switch (moduleName) {
                case "picture":
                    spaceUserAuthContext.setPictureId(id);
                    break;
                case "spaceUser":
                    spaceUserAuthContext.setSpaceUserId(id);
                    break;
                case "space":
                    spaceUserAuthContext.setSpaceId(id);
                    break;
                default:
            }
        }

        return spaceUserAuthContext;
    }


    /**
     * 校验是否为空
     * @param object
     * @return
     */
    private boolean isAllFieldsNull(Object object) {
        if (object == null) {
            return true; // 对象本身为空
        }
        // 获取所有字段并判断是否所有字段都为空
        return Arrays.stream(ReflectUtil.getFields(object.getClass()))
                // 获取字段值
                .map(field -> ReflectUtil.getFieldValue(object, field))
                // 检查是否所有字段都为空
                .allMatch(ObjectUtil::isEmpty);
    }

}
