package com.bookmall.book.vo;

import lombok.Data;

@Data
//普通分类列表
public class CategoryVO {

    private Long id;
    private String name;
    private Long parentId;
    private Integer sort;

    public CategoryVO() {
    }

    public CategoryVO(Long id, String name, Long parentId, Integer sort) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
        this.sort = sort;
    }

}