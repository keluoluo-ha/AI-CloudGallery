package com.hhk.picturebacked.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hhk.picturebacked.model.dto.product.ProductAddRequest;
import com.hhk.picturebacked.model.dto.product.ProductEditRequest;
import com.hhk.picturebacked.model.dto.product.ProductQueryRequest;
import com.hhk.picturebacked.model.entity.Product;
import com.hhk.picturebacked.model.entity.User;

/**
 * 商品（SKU）服务
 */
public interface ProductService extends IService<Product> {

    /**
     * 新增商品
     *
     * @param productAddRequest 商品新增请求
     * @param loginUser         登录用户
     * @return 新商品 id
     */
    long addProduct(ProductAddRequest productAddRequest, User loginUser);

    /**
     * 编辑商品
     *
     * @param productEditRequest 商品编辑请求
     * @param loginUser          登录用户
     */
    void editProduct(ProductEditRequest productEditRequest, User loginUser);

    /**
     * 删除商品
     *
     * @param id        商品 id
     * @param loginUser 登录用户
     */
    void deleteProduct(Long id, User loginUser);

    /**
     * 数据校验
     *
     * @param product 商品
     * @param add     是否为新增
     */
    void validProduct(Product product, boolean add);

    /**
     * 构造查询条件
     *
     * @param productQueryRequest 查询请求
     * @return 查询条件
     */
    com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Product> getQueryWrapper(ProductQueryRequest productQueryRequest);

    /**
     * 分页获取商品列表
     *
     * @param productQueryRequest 查询请求
     * @return 分页结果
     */
    Page<Product> listProductByPage(ProductQueryRequest productQueryRequest);
}
