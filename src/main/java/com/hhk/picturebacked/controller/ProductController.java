package com.hhk.picturebacked.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hhk.picturebacked.auth.annotation.SaSpaceCheckPermission;
import com.hhk.picturebacked.auth.model.SpaceUserPermissionConstant;
import com.hhk.picturebacked.commom.BaseResponse;
import com.hhk.picturebacked.commom.DeleteRequest;
import com.hhk.picturebacked.commom.ResultUtils;
import com.hhk.picturebacked.exception.BusinessException;
import com.hhk.picturebacked.exception.ErrorCode;
import com.hhk.picturebacked.exception.ThrowUtils;
import com.hhk.picturebacked.model.dto.product.ProductAddRequest;
import com.hhk.picturebacked.model.dto.product.ProductEditRequest;
import com.hhk.picturebacked.model.dto.product.ProductQueryRequest;
import com.hhk.picturebacked.model.dto.picture.PictureQueryRequest;
import com.hhk.picturebacked.model.entity.Picture;
import com.hhk.picturebacked.model.entity.Product;
import com.hhk.picturebacked.model.entity.User;
import com.hhk.picturebacked.model.vo.PictureVO;
import com.hhk.picturebacked.service.PictureService;
import com.hhk.picturebacked.service.ProductService;
import com.hhk.picturebacked.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 商品（SKU）管理
 * 一个 Space 对应一个店铺，一个 product 对应店铺下的一个 SKU
 */
@RestController
@RequestMapping("/product")
@Slf4j
public class ProductController {

    @Resource
    private ProductService productService;

    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;

    /**
     * 新增商品
     */
    @PostMapping("/add")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Long> addProduct(@RequestBody ProductAddRequest productAddRequest,
                                         HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        long id = productService.addProduct(productAddRequest, loginUser);
        return ResultUtils.success(id);
    }

    /**
     * 编辑商品
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editProduct(@RequestBody ProductEditRequest productEditRequest,
                                             HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        productService.editProduct(productEditRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 删除商品
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_DELETE)
    public BaseResponse<Boolean> deleteProduct(@RequestBody DeleteRequest deleteRequest,
                                               HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        productService.deleteProduct(deleteRequest.getId(), loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取商品
     */
    @GetMapping("/get")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<Product> getProductById(@RequestParam("id") long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        Product product = productService.getById(id);
        ThrowUtils.throwIf(product == null, ErrorCode.NOT_FOUND_ERROR, "商品不存在");
        return ResultUtils.success(product);
    }

    /**
     * 分页获取商品列表
     */
    @PostMapping("/list/page")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<Page<Product>> listProductByPage(@RequestBody ProductQueryRequest productQueryRequest) {
        ThrowUtils.throwIf(productQueryRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(productService.listProductByPage(productQueryRequest));
    }

    /**
     * 获取店铺下的商品列表（不分页，供前端下拉选择）
     */
    @PostMapping("/list")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<List<Product>> listProduct(@RequestBody ProductQueryRequest productQueryRequest) {
        ThrowUtils.throwIf(productQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Long spaceId = productQueryRequest.getSpaceId();
        ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR, "必须指定空间 id");
        List<Product> productList = productService.list(productService.getQueryWrapper(productQueryRequest));
        return ResultUtils.success(productList);
    }

    /**
     * 查询某个商品（SKU）下的全部素材
     * 供缺图看板、素材统计使用
     */
    @PostMapping("/picture/list")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<List<PictureVO>> listProductPicture(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                            HttpServletRequest request) {
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Long productId = pictureQueryRequest.getProductId();
        ThrowUtils.throwIf(productId == null || productId <= 0, ErrorCode.PARAMS_ERROR, "必须指定商品 id");
        // 查询商品，拿到所属空间做权限校验
        Product product = productService.getById(productId);
        ThrowUtils.throwIf(product == null, ErrorCode.NOT_FOUND_ERROR, "商品不存在");
        // 强制按商品所属空间过滤，避免跨店查图
        pictureQueryRequest.setProductId(productId);
        pictureQueryRequest.setSpaceId(product.getSpaceId());
        Page<Picture> picturePage = pictureService.page(
                new Page<>(1, 100), pictureService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(pictureService.getpictureVOList(picturePage, request).getRecords());
    }
}
