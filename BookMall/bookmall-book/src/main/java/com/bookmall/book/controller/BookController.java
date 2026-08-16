package com.bookmall.book.controller;

import com.bookmall.book.dto.BookCreateRequest;
import com.bookmall.book.dto.BookSearchRequest;
import com.bookmall.book.dto.BookUpdateRequest;

import com.bookmall.book.service.BookService;
import com.bookmall.book.service.CategoryService;

import com.bookmall.book.vo.BookDetailVO;
import com.bookmall.book.vo.BookVO;
import com.bookmall.book.vo.CategoryTreeVO;
import com.bookmall.book.vo.CategoryVO;
import com.bookmall.common.result.PageResult;
import com.bookmall.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/books")
public class BookController {

    private final BookService bookService;
    private final CategoryService categoryService;

    //依赖注入
    public BookController(BookService bookService, CategoryService categoryService) {
        this.bookService = bookService;
        this.categoryService = categoryService;
    }

    //测试接口
    @GetMapping("/hello")
    public Result<String> hello() {
        return Result.success("bookmall-book is running");
    }

    //查询所有图书
    @GetMapping
    public Result<List<BookVO>> listBooks() {
        return Result.success(bookService.listBooks());
    }

    //根据图书 ID 查询图书详情
    @GetMapping("/{id}")
    public Result<BookDetailVO> getBookById(@PathVariable("id") Long id) {
        BookDetailVO book = bookService.getBookById(id);
        if (book == null) {
            return Result.fail(404, "图书不存在");
        }
        return Result.success(book);
    }

    @GetMapping("/search")
    //图书条件搜索
    // 1.根据关键词`keyword`模糊搜索，图书名称、作者等关键词
    // 2.根据分类`categoryId`筛选图书
    //@RequestParam：接收 URL 问号 ? 后面的查询参数
    public Result<List<BookVO>> searchBooks(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "categoryId", required = false) Long categoryId) {

        //BookSearchRequest 是 DTO，它专门用来封装前端传来的搜索条件
        BookSearchRequest request = new BookSearchRequest();
        request.setKeyword(keyword);
        request.setCategoryId(categoryId);

        return Result.success(bookService.searchBooks(request));
    }

    //图书分页查询
    @GetMapping("/page")
    public Result<PageResult<BookVO>> pageBooks(
            // 页码
            @RequestParam(name = "pageNum", required = false) Integer pageNum,
            // 每页显示的图书数量
            @RequestParam(name = "pageSize", required = false) Integer pageSize,
            // 图书搜索关键词
            @RequestParam(name = "keyword", required = false) String keyword,
            // 分类ID，按照分类筛选
            @RequestParam(name = "categoryId", required = false) Long categoryId) {

        return Result.success(bookService.pageBooks(pageNum, pageSize, keyword, categoryId));
    }

    //新增图书
    @PostMapping
    public Result<BookDetailVO> createBook(@Valid @RequestBody BookCreateRequest request) {
        return Result.success(bookService.createBook(request));
    }

    //修改图书信息
    @PutMapping("/{id}")
    public Result<BookDetailVO> updateBook(@PathVariable("id") Long id,
                                           @Valid @RequestBody BookUpdateRequest request) {
        BookDetailVO book = bookService.updateBook(id, request);
        if (book == null) {
            return Result.fail(404, "图书不存在");
        }
        return Result.success(book);
    }

    //删除图书
    @DeleteMapping("/{id}")
    public Result<String> deleteBook(@PathVariable("id") Long id) {
        boolean deleted = bookService.deleteBook(id);
        if (!deleted) {
            return Result.fail(404, "图书不存在");
        }
        return Result.success("删除成功");
    }

    //图书上下架
    @PutMapping("/{id}/status")
    public Result<String> updateBookStatus(@PathVariable("id") Long id,
                                           @RequestParam("status") Integer status) {
        boolean updated = bookService.updateBookStatus(id, status);
        if (!updated) {
            return Result.fail(404, "图书不存在");
        }
        return Result.success("状态更新成功");
    }

    //获取普通分类列表
    @GetMapping("/categories")
    public Result<List<CategoryVO>> listCategories() {
        return Result.success(categoryService.listCategories());
    }

    //获取分类树
    @GetMapping("/categories/tree")
    public Result<List<CategoryTreeVO>> listCategoryTree() {
        return Result.success(categoryService.listCategoryTree());
    }
}