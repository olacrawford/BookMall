package com.bookmall.book.service;

import com.bookmall.book.dto.BookCreateRequest;
import com.bookmall.book.dto.BookSearchRequest;
import com.bookmall.book.dto.BookUpdateRequest;
import com.bookmall.book.vo.BookDetailVO;
import com.bookmall.book.vo.BookVO;
import com.bookmall.book.vo.CategoryTreeVO;
import com.bookmall.book.vo.CategoryVO;
import com.bookmall.common.result.PageResult;

import java.util.List;

public interface BookService {

    List<BookVO> listBooks();

    BookDetailVO getBookById(Long id);

    List<BookVO> searchBooks(BookSearchRequest request);

    PageResult<BookVO> pageBooks(Integer pageNum, Integer pageSize, String keyword, Long categoryId);

    BookDetailVO createBook(BookCreateRequest request);

    BookDetailVO updateBook(Long id, BookUpdateRequest request);

    boolean deleteBook(Long id);

    boolean updateBookStatus(Long id, Integer status);
}