package com.bookmall.order.client;

import com.bookmall.common.result.Result;
import com.bookmall.order.client.dto.BookSnapshot;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "book")
public interface BookClient {

    @GetMapping("/books/{id}")
    Result<BookSnapshot> getBookById(@PathVariable("id") Long bookId);
}
