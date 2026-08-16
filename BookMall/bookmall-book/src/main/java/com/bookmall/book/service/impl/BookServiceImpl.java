package com.bookmall.book.service.impl;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bookmall.book.constant.CacheKeys;
import com.bookmall.book.dto.BookCreateRequest;
import com.bookmall.book.dto.BookSearchRequest;
import com.bookmall.book.dto.BookUpdateRequest;
import com.bookmall.book.entity.Book;
import com.bookmall.book.entity.Category;
import com.bookmall.book.exception.SentinelBlockedException;
import com.bookmall.book.mapper.BookMapper;
import com.bookmall.book.mapper.CategoryMapper;
import com.bookmall.book.service.BookService;
import com.bookmall.book.service.support.CacheSupport;
import com.bookmall.book.vo.BookDetailVO;
import com.bookmall.book.vo.BookVO;
import com.bookmall.common.result.PageResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BookServiceImpl implements BookService {

    private final BookMapper bookMapper;
    private final CategoryMapper categoryMapper;
    private final CacheSupport cacheSupport;

    public BookServiceImpl(BookMapper bookMapper, CategoryMapper categoryMapper, CacheSupport cacheSupport) {
        this.bookMapper = bookMapper;
        this.categoryMapper = categoryMapper;
        this.cacheSupport = cacheSupport;
    }

    @Override
    @SentinelResource(value = "/books", blockHandler = "handleListBlocked")
    public List<BookVO> listBooks() {
        Object cached = cacheSupport.get(CacheKeys.BOOK_LIST);
        if (cached instanceof List<?> list && !list.isEmpty()) {
            return list.stream().map(item -> (BookVO) item).collect(Collectors.toList());
        }
        if (cacheSupport.isEmptyValue(cached)) {
            return List.of();
        }

        List<BookVO> books = bookMapper.selectList(
                new LambdaQueryWrapper<Book>().eq(Book::getStatus, 1)
        ).stream().map(this::toBookVO).collect(Collectors.toList());

        if (books.isEmpty()) {
            cacheSupport.putEmpty(CacheKeys.BOOK_LIST);
        } else {
            cacheSupport.putBookList(books);
        }
        return books;
    }

    @Override
    @SentinelResource(value = "/books/{id}", blockHandler = "handleDetailBlocked")
    public BookDetailVO getBookById(Long id) {
        String key = CacheKeys.bookDetail(id);
        Object cached = cacheSupport.get(key);
        if (cached instanceof BookDetailVO vo) {
            return vo;
        }
        if (cacheSupport.isEmptyValue(cached)) {
            return null;
        }

        Book book = bookMapper.selectOne(
                new LambdaQueryWrapper<Book>()
                        .eq(Book::getId, id)
                        .eq(Book::getStatus, 1)
        );
        if (book == null) {
            cacheSupport.putEmpty(key);
            return null;
        }

        BookDetailVO vo = new BookDetailVO(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getPrice(),
                book.getCategoryId(),
                book.getCoverUrl(),
                book.getDescription(),
                book.getStatus()
        );
        cacheSupport.putBookDetail(id, vo);
        return vo;
    }

    @Override
    @SentinelResource(value = "/books/search", blockHandler = "handleSearchBlocked")
    public List<BookVO> searchBooks(BookSearchRequest request) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<Book>()
                .eq(Book::getStatus, 1)
                .like(request.getKeyword() != null && !request.getKeyword().isBlank(),
                        Book::getTitle, request.getKeyword());

        Set<Long> categoryIds = collectCategoryIds(request.getCategoryId());
        if (!categoryIds.isEmpty()) {
            // 选中父分类时，递归把全部子分类一起纳入搜索范围。
            wrapper.in(Book::getCategoryId, categoryIds);
        }

        List<Book> books = bookMapper.selectList(wrapper);

        return books.stream()
                .map(this::toBookVO)
                .collect(Collectors.toList());
    }

    @Override
    @SentinelResource(value = "/books/page", blockHandler = "handlePageBlocked")
    public PageResult<BookVO> pageBooks(Integer pageNum, Integer pageSize, String keyword, Long categoryId) {
        long currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long size = pageSize == null || pageSize < 1 ? 10 : pageSize;

        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<Book>()
                .eq(Book::getStatus, 1)
                .like(keyword != null && !keyword.isBlank(), Book::getTitle, keyword)
                .orderByDesc(Book::getId);

        Set<Long> categoryIds = collectCategoryIds(categoryId);
        if (!categoryIds.isEmpty()) {
            wrapper.in(Book::getCategoryId, categoryIds);
        }

        Page<Book> page = new Page<>(currentPage, size);
        bookMapper.selectPage(page, wrapper);

        List<BookVO> records = page.getRecords().stream()
                .map(this::toBookVO)
                .collect(Collectors.toList());

        return new PageResult<>(records, page.getTotal(), page.getPages(), page.getCurrent(), page.getSize());
    }

    @Override
    public BookDetailVO createBook(BookCreateRequest request) {
        BookDetailVO vo = saveBook(request, null);
        cacheSupport.clearBookCaches();
        return vo;
    }

    @Override
    public BookDetailVO updateBook(Long id, BookUpdateRequest request) {
        Book book = bookMapper.selectById(id);
        if (book == null) {
            return null;
        }

        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setPrice(request.getPrice());
        book.setCategoryId(request.getCategoryId());
        book.setCoverUrl(request.getCoverUrl());
        book.setDescription(request.getDescription());
        book.setStatus(request.getStatus());
        book.setUpdateTime(LocalDateTime.now());

        bookMapper.updateById(book);
        // 图书更新后统一失效列表、分类树和当前详情缓存。
        cacheSupport.clearBookCaches();
        cacheSupport.clearBookDetail(id);

        return new BookDetailVO(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getPrice(),
                book.getCategoryId(),
                book.getCoverUrl(),
                book.getDescription(),
                book.getStatus()
        );
    }

    @Override
    public boolean deleteBook(Long id) {
        Book book = bookMapper.selectById(id);
        if (book == null) {
            return false;
        }

        bookMapper.deleteById(id);
        cacheSupport.clearBookCaches();
        cacheSupport.clearBookDetail(id);
        return true;
    }

    @Override
    public boolean updateBookStatus(Long id, Integer status) {
        Book book = bookMapper.selectById(id);
        if (book == null) {
            return false;
        }

        book.setStatus(status);
        book.setUpdateTime(LocalDateTime.now());
        bookMapper.updateById(book);
        cacheSupport.clearBookCaches();
        cacheSupport.clearBookDetail(id);
        return true;
    }

    public List<BookVO> handleListBlocked(BlockException e) {
        throw new SentinelBlockedException("图书列表请求过于频繁，请稍后再试", e);
    }

    public BookDetailVO handleDetailBlocked(Long id, BlockException e) {
        throw new SentinelBlockedException("图书详情请求过于频繁，请稍后再试", e);
    }

    public List<BookVO> handleSearchBlocked(BookSearchRequest request, BlockException e) {
        throw new SentinelBlockedException("图书搜索请求过于频繁，请稍后再试", e);
    }

    public PageResult<BookVO> handlePageBlocked(Integer pageNum, Integer pageSize, String keyword, Long categoryId, BlockException e) {
        throw new SentinelBlockedException("图书分页请求过于频繁，请稍后再试", e);
    }

    private BookDetailVO saveBook(BookCreateRequest request, Long id) {
        Book book = new Book();
        book.setId(id);
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setPrice(request.getPrice());
        book.setCategoryId(request.getCategoryId());
        book.setCoverUrl(request.getCoverUrl());
        book.setDescription(request.getDescription());
        book.setStatus(1);
        book.setCreateTime(LocalDateTime.now());
        book.setUpdateTime(LocalDateTime.now());

        bookMapper.insert(book);
        return new BookDetailVO(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getPrice(),
                book.getCategoryId(),
                book.getCoverUrl(),
                book.getDescription(),
                book.getStatus()
        );
    }

    private BookVO toBookVO(Book book) {
        return new BookVO(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getPrice(),
                book.getCoverUrl(),
                book.getCategoryId()
        );
    }

    private Set<Long> collectCategoryIds(Long categoryId) {
        Set<Long> categoryIds = new HashSet<>();
        if (categoryId == null) {
            return categoryIds;
        }

        categoryIds.add(categoryId);
        collectChildren(categoryId, categoryIds);
        return categoryIds;
    }

    private void collectChildren(Long parentId, Set<Long> categoryIds) {
        List<Category> children = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getStatus, 1)
                        .eq(Category::getParentId, parentId)
        );

        for (Category child : children) {
            if (categoryIds.add(child.getId())) {
                collectChildren(child.getId(), categoryIds);
            }
        }
    }
}
