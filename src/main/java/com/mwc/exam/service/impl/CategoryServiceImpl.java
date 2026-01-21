package com.mwc.exam.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mwc.exam.entity.Category;
import com.mwc.exam.entity.Question;
import com.mwc.exam.mapper.CategoryMapper;
import com.mwc.exam.mapper.QuestionMapper;
import com.mwc.exam.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * 分类服务实现类，提供分类相关的业务逻辑处理
 */
@Slf4j
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {
    @Autowired
    private QuestionMapper questionMapper;

    /**
     * 查询所有分类信息，并统计每个分类下的题目数量
     * @return 包含题目数量的分类列表，按排序字段升序排列
     */
    @Override
    public List<Category> getAllCategories() {
        // 查询所有分类信息并按排序字段升序排列
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(Category::getSort);
        List<Category> categoryList = list(queryWrapper);

        // 获取各分类下的题目数量统计
        List<Map<String, Long>> countList = questionMapper.getCategoryCount();

        // 将统计结果转换为Map，以分类ID为键，题目数量为值
        Map<Long, Long> collect = countList.stream()
                .collect(Collectors.toMap(k -> k.get("categoryId"), v -> v.get("count")));

        // 为每个分类设置对应的题目数量
        for (Category category : categoryList) {
            Long id = category.getId();
            category.setCount(collect.getOrDefault(id, 0L));
        }

        return categoryList;
    }

    /**
     * 查询所有分类信息并构建分类树结构，同时统计每个分类及其子分类的题目总数
     * @return 包含层级关系和题目数量的分类列表（仅包含一级分类）
     */
    @Override
    public List<Category> getAllCategoriesTree() {
        // 查询所有分类信息并按排序字段升序排列
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(Category::getSort);
        List<Category> categoryList = list(queryWrapper);

        // 获取各分类下的题目数量统计
        List<Map<String, Long>> countList = questionMapper.getCategoryCount();

        // 将统计结果转换为Map，以分类ID为键，题目数量为值
        Map<Long, Long> collect = countList.stream()
                .collect(Collectors.toMap(k -> k.get("categoryId"), v -> v.get("count")));

        // 为每个分类设置直接关联的题目数量
        for (Category category : categoryList) {
            Long id = category.getId();
            category.setCount(collect.getOrDefault(id, 0L));
        }

        // 按父分类ID对分类进行分组，构建父子分类映射关系
        Map<Long, List<Category>> categoryParent = categoryList.stream().collect(Collectors.groupingBy(Category::getParentId));

        // 筛选一级分类（父ID为0的分类）
        List<Category> collectParent = categoryList.stream().filter(category -> category.getParentId() == 0).collect(Collectors.toList());

        // 构建分类树结构并计算每个分类及其子分类的题目总数
        for (Category category : collectParent) {
            category.setChildren(categoryParent.getOrDefault(category.getId(), new ArrayList<>()));
            category.setCount(category.getCount() + category.getChildren().stream().mapToLong(Category::getCount).sum());
        }

        return collectParent;
    }

    /**
     * 添加新的分类
     * @param category 待添加的分类对象
     * @throws RuntimeException 当同级分类名称已存在时抛出异常
     */
    @Override
    public void addCategory(Category category) {
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        // 判断该分类是否已存在（同一父分类下不能有相同名称的分类）
        queryWrapper.eq(Category::getParentId, category.getParentId());
        queryWrapper.eq(Category::getName, category.getName());
        if (count(queryWrapper) > 0) {
            throw new RuntimeException("该分类已存在");
        }else {
            save(category);
        }
    }

    /**
     * 更新分类信息
     * @param category 待更新的分类对象
     * @throws RuntimeException 当同级分类名称已存在时抛出异常
     */
    @Override
    public void updateCategory(Category category) {
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        // 判断该分类是否已存在（同一父分类下不能有相同名称的分类，排除自身）
        queryWrapper.eq(Category::getParentId, category.getParentId());
        queryWrapper.eq(Category::getName, category.getName());
        queryWrapper.ne(Category::getId, category.getId());
        if (count(queryWrapper) > 0) {
            throw new RuntimeException("该分类已存在");
        }else {
            updateById(category);
        }
    }

    /**
     * 根据ID删除分类
     * @param id 分类ID
     */
    @Override
    public void removeCategoryById(Long id) {
        // 获取要删除的分类信息
        Category byId = getById(id);
        if (byId == null) {
            throw new RuntimeException("分类不存在");
        }
        if (byId.getParentId() == 0) {
            throw new RuntimeException("不能删除顶级分类");
        }

        // 检查该分类下是否有关联的题目
        if (questionMapper.selectCount(new LambdaQueryWrapper<Question>().eq(Question::getCategoryId, id)) > 0) {
            throw new RuntimeException("该分类下有题目，请先删除题目");
        }

        // 执行删除操作
        removeById(id);
    }

}
