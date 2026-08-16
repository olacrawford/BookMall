package com.bookmall.book.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
//分类树
public class CategoryTreeVO {

    private Long id;
    private String name;
    private Long parentId;
    private Integer sort;
    private List<CategoryTreeVO> children = new ArrayList<>();

    public CategoryTreeVO() {
    }

    public CategoryTreeVO(Long id, String name, Long parentId, Integer sort) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
        this.sort = sort;
    }

}