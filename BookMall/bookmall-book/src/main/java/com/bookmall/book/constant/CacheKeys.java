package com.bookmall.book.constant;

public final class CacheKeys {

    public static final String BOOK_LIST = "book:list";
    public static final String BOOK_DETAIL_PREFIX = "book:detail:";
    public static final String CATEGORY_TREE = "book:category:tree";
    public static final String EMPTY_VALUE = "__EMPTY__";

    private CacheKeys() {
    }

    public static String bookDetail(Long id) {
        return BOOK_DETAIL_PREFIX + id;
    }
}
