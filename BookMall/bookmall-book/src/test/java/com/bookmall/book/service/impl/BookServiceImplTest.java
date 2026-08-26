package com.bookmall.book.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bookmall.book.entity.Book;
import com.bookmall.book.mapper.BookMapper;
import com.bookmall.book.vo.BookDetailVO;
import com.bookmall.book.vo.BookVO;
import com.bookmall.common.result.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

    @Mock
    private BookMapper bookMapper;

    private BookServiceImpl bookService;

    @BeforeEach
    void setUp() {
        bookService = new BookServiceImpl(bookMapper);
    }

    @Test
    void listBooks_returnsMappedBookList() {
        when(bookMapper.selectList(any())).thenReturn(List.of(book()));

        List<BookVO> result = bookService.listBooks();

        assertEquals(1, result.size());
        assertEquals("Java核心技术", result.get(0).getTitle());
        assertEquals(new BigDecimal("149.00"), result.get(0).getPrice());
        verify(bookMapper).selectList(any());
    }

    @Test
    void getBookById_returnsDetail_whenBookExists() {
        when(bookMapper.selectOne(any())).thenReturn(book());

        BookDetailVO result = bookService.getBookById(1L);

        assertEquals("Java核心技术", result.getTitle());
        assertEquals("凯·霍斯特曼", result.getAuthor());
        assertEquals(2L, result.getCategoryId());
    }

    @Test
    void getBookById_returnsNull_whenBookNotFound() {
        when(bookMapper.selectOne(any())).thenReturn(null);

        assertNull(bookService.getBookById(999L));
    }

    @Test
    void pageBooks_returnsPageResult() {
        Page<Book> page = new Page<>(1, 10);
        when(bookMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<Book> target = invocation.getArgument(0);
            target.setRecords(List.of(book()));
            target.setTotal(1);
            return target;
        });

        PageResult<BookVO> result = bookService.pageBooks(1, 10, "Java", 2L);

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals("Java核心技术", result.getRecords().get(0).getTitle());
    }

    private Book book() {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Java核心技术");
        book.setAuthor("凯·霍斯特曼");
        book.setPrice(new BigDecimal("149.00"));
        book.setCategoryId(2L);
        book.setCoverUrl("https://example.com/book.jpg");
        book.setDescription("Java技术参考书籍");
        book.setStatus(1);
        return book;
    }
}
