package com.bookmall.book.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookmall.book.constant.CacheKeys;
import com.bookmall.book.entity.Category;
import com.bookmall.book.mapper.CategoryMapper;
import com.bookmall.book.service.CategoryService;
import com.bookmall.book.service.support.CacheSupport;
import com.bookmall.book.vo.CategoryTreeVO;
import com.bookmall.book.vo.CategoryVO;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final CacheSupport cacheSupport;

    public CategoryServiceImpl(CategoryMapper categoryMapper, CacheSupport cacheSupport) {
        this.categoryMapper = categoryMapper;
        this.cacheSupport = cacheSupport;
    }

    @Override
    public List<CategoryVO> listCategories() {

        List<Category> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getStatus, 1)
                        .orderByAsc(Category::getSort)
        );

        return categories.stream()
                .map(category -> new CategoryVO(
                        category.getId(),
                        category.getName(),
                        category.getParentId(),
                        category.getSort()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<CategoryTreeVO> listCategoryTree() {
        Object cached = cacheSupport.get(CacheKeys.CATEGORY_TREE);
        if (cached instanceof List<?> list && !list.isEmpty()) {
            return list.stream().map(item -> (CategoryTreeVO) item).collect(Collectors.toList());
        }
        if (cacheSupport.isEmptyValue(cached)) {
            return List.of();
        }

        List<Category> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getStatus, 1)
                        .orderByAsc(Category::getSort)
        );

        List<CategoryTreeVO> allNodes = categories.stream()
                .map(category -> new CategoryTreeVO(
                        category.getId(),
                        category.getName(),
                        category.getParentId(),
                        category.getSort()
                ))
                .collect(Collectors.toList());

        List<CategoryTreeVO> roots = allNodes.stream()
                .filter(node -> node.getParentId() == null || node.getParentId() == 0)
                .sorted(Comparator.comparing(CategoryTreeVO::getSort))
                .collect(Collectors.toList());

        for (CategoryTreeVO root : roots) {
            // 把数据库平铺分类组装成前端更容易消费的树形结构。
            buildChildren(root, allNodes);
        }

        if (roots.isEmpty()) {
            cacheSupport.putEmpty(CacheKeys.CATEGORY_TREE);
        } else {
            cacheSupport.putCategoryTree(roots);
        }
        return roots;
    }

    private void buildChildren(CategoryTreeVO parent, List<CategoryTreeVO> allNodes) {
        List<CategoryTreeVO> children = allNodes.stream()
                .filter(node -> parent.getId().equals(node.getParentId()))
                .sorted(Comparator.comparing(CategoryTreeVO::getSort))
                .collect(Collectors.toList());
        parent.setChildren(children);
        for (CategoryTreeVO child : children) {
            buildChildren(child, allNodes);
        }
    }
}
