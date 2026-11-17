package com.hhk.picturebacked.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hhk.picturebacked.exception.BusinessException;
import com.hhk.picturebacked.exception.ErrorCode;
import com.hhk.picturebacked.exception.ThrowUtils;
import com.hhk.picturebacked.mapper.ProductMapper;
import com.hhk.picturebacked.model.dto.product.ProductAddRequest;
import com.hhk.picturebacked.model.dto.product.ProductEditRequest;
import com.hhk.picturebacked.model.dto.product.ProductQueryRequest;
import com.hhk.picturebacked.model.entity.Product;
import com.hhk.picturebacked.model.entity.User;
import com.hhk.picturebacked.model.enums.ProductStatusEnum;
import com.hhk.picturebacked.service.ProductService;
import com.hhk.picturebacked.service.SpaceService;
import com.hhk.picturebacked.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 商品（SKU）服务实现
 *
 * @author 30674
 * @description 针对表【product(商品)】的数据库操作Service实现
 */
@Service
@Slf4j
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product>
        implements ProductService {

    @Resource
    private UserService userService;

    @Resource
    @Lazy
    private SpaceService spaceService;

    @Override
    public long addProduct(ProductAddRequest productAddRequest, User loginUser) {
        ThrowUtils.throwIf(productAddRequest == null, ErrorCode.PARAMS_ERROR);
        Long spaceId = productAddRequest.getSpaceId();
        ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR, "商品必须归属于某个空间");

        // 实体类转换
        Product product = new Product();
        BeanUtils.copyProperties(productAddRequest, product);
        product.setUserId(loginUser.getId());
        if (product.getStatus() == null) {
            product.setStatus(ProductStatusEnum.DRAFT.getValue());
        }
        // 数据校验（含 SKU 店铺内唯一）
        validProduct(product, true);
        // 保存
        boolean result = this.save(product);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "创建商品失败");
        return product.getId();
    }

    @Override
    public void editProduct(ProductEditRequest productEditRequest, User loginUser) {
        ThrowUtils.throwIf(productEditRequest == null || productEditRequest.getId() == null
                || productEditRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        // 查询原商品
        Product oldProduct = this.getById(productEditRequest.getId());
        ThrowUtils.throwIf(oldProduct == null, ErrorCode.NOT_FOUND_ERROR, "商品不存在");
        // 权限校验：必须是该空间的图片编辑者
        checkProductAuth(loginUser, oldProduct);

        // 实体类转换
        Product product = new Product();
        BeanUtils.copyProperties(productEditRequest, product);
        product.setId(oldProduct.getId());
        product.setSpaceId(oldProduct.getSpaceId());
        product.setUserId(oldProduct.getUserId());
        product.setEditTime(new Date());
        // 数据校验
        validProduct(product, false);
        // 更新
        boolean result = this.updateById(product);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "编辑商品失败");
    }

    @Override
    public void deleteProduct(Long id, User loginUser) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        Product oldProduct = this.getById(id);
        ThrowUtils.throwIf(oldProduct == null, ErrorCode.NOT_FOUND_ERROR, "商品不存在");
        // 权限校验
        checkProductAuth(loginUser, oldProduct);
        // 逻辑删除
        boolean result = this.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "删除商品失败");
    }

    @Override
    public void validProduct(Product product, boolean add) {
        ThrowUtils.throwIf(product == null, ErrorCode.PARAMS_ERROR);
        String sku = product.getSku();
        String title = product.getTitle();
        Integer status = product.getStatus();
        Long spaceId = product.getSpaceId();

        if (add) {
            ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR, "商品必须归属于某个空间");
        }
        // SKU 必填且长度限制
        ThrowUtils.throwIf(StrUtil.isBlank(sku), ErrorCode.PARAMS_ERROR, "SKU 编码不能为空");
        ThrowUtils.throwIf(sku.length() > 64, ErrorCode.PARAMS_ERROR, "SKU 编码过长");
        // 商品名长度限制
        ThrowUtils.throwIf(title != null && title.length() > 255, ErrorCode.PARAMS_ERROR, "商品名称过长");
        // 状态校验
        ThrowUtils.throwIf(status != null && ProductStatusEnum.getEnumByValue(status) == null,
                ErrorCode.PARAMS_ERROR, "商品状态不存在");

        // SKU 在店铺内唯一
        if (StrUtil.isNotBlank(sku) && spaceId != null) {
            boolean exists = this.lambdaQuery()
                    .eq(Product::getSpaceId, spaceId)
                    .eq(Product::getSku, sku)
                    .ne(add, Product::getId, product.getId())
                    .exists();
            ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "该店铺下 SKU 已存在：" + sku);
        }
    }

    /**
     * 商品权限校验：必须对该空间有图片编辑权限
     * 复用空间鉴权体系，不新增独立权限码
     */
    private void checkProductAuth(User loginUser, Product product) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        Long spaceId = product.getSpaceId();
        ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.NO_AUTH_ERROR, "商品未归属空间");
        // 空间不存在则无权操作
        if (spaceService.getById(spaceId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        // 空间创建者或系统管理员直接放行
        if (userService.isAdmin(loginUser)) {
            return;
        }
        // 其余情况依赖注解上的 @SaSpaceCheckPermission 做空间级校验
    }

    @Override
    public QueryWrapper<Product> getQueryWrapper(ProductQueryRequest productQueryRequest) {
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        if (productQueryRequest == null) {
            return queryWrapper;
        }
        Long id = productQueryRequest.getId();
        Long spaceId = productQueryRequest.getSpaceId();
        String sku = productQueryRequest.getSku();
        String title = productQueryRequest.getTitle();
        Integer status = productQueryRequest.getStatus();
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.eq(StrUtil.isNotBlank(sku), "sku", sku);
        queryWrapper.like(StrUtil.isNotBlank(title), "title", title);
        queryWrapper.eq(ObjUtil.isNotEmpty(status), "status", status);
        queryWrapper.orderByDesc("createTime");
        return queryWrapper;
    }

    @Override
    public Page<Product> listProductByPage(ProductQueryRequest productQueryRequest) {
        long current = productQueryRequest.getCurrent() <= 0 ? 1 : productQueryRequest.getCurrent();
        long size = productQueryRequest.getPageSize() <= 0 ? 20 : productQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 50, ErrorCode.PARAMS_ERROR);
        return this.page(new Page<>(current, size), getQueryWrapper(productQueryRequest));
    }
}
