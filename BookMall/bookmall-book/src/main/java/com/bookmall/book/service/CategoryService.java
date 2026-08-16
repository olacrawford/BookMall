package com.bookmall.book.service;

import com.bookmall.book.vo.CategoryTreeVO;
import com.bookmall.book.vo.CategoryVO;

import java.util.List;

public interface CategoryService {

    List<CategoryVO> listCategories();

    List<CategoryTreeVO> listCategoryTree();
}