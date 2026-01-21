package com.mwc.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mwc.exam.entity.Category;

import java.util.List;

public interface CategoryService extends IService<Category> {


    List<Category> getAllCategories();
    //获取所有分类树
    List<Category> getAllCategoriesTree();
    //添加分类
    void addCategory(Category category);
    //修改分类
    void updateCategory(Category category);
    //删除分类
    void removeCategoryById(Long id);
}