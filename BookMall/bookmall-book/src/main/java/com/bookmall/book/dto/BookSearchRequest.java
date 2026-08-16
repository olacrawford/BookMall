package com.bookmall.book.dto;

import lombok.Data;

@Data
public class BookSearchRequest {

    private String keyword;
    private Long categoryId;

}