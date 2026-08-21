package com.bookmall.book.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookmall.book.entity.Category;
import com.bookmall.book.mapper.CategoryMapper;
import com.bookmall.book.service.CategoryService;
import com.bookmall.book.vo.CategoryVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    public CategoryServiceImpl(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    @Override
    public List<CategoryVO> listCategories() {
        return categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getStatus, 1)
                        .orderByAsc(Category::getSort)
        ).stream()
                .map(category -> new CategoryVO(
                        category.getId(),
                        category.getName(),
                        category.getSort()
                ))
                .collect(Collectors.toList());
    }
}
