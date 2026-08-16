package com.bookmall.book.service.support;

import com.bookmall.book.config.CacheProperties;
import com.bookmall.book.constant.CacheKeys;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class CacheSupport {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheProperties cacheProperties;

    public CacheSupport(RedisTemplate<String, Object> redisTemplate, CacheProperties cacheProperties) {
        this.redisTemplate = redisTemplate;
        this.cacheProperties = cacheProperties;
    }

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void putBookList(Object value) {
        put(CacheKeys.BOOK_LIST, value, cacheProperties.getBookListTtlMinutes());
    }

    public void putBookDetail(Long id, Object value) {
        put(CacheKeys.bookDetail(id), value, cacheProperties.getBookDetailTtlMinutes());
    }

    public void putCategoryTree(Object value) {
        put(CacheKeys.CATEGORY_TREE, value, cacheProperties.getCategoryTreeTtlMinutes());
    }

    public void putEmpty(String key) {
        put(key, CacheKeys.EMPTY_VALUE, cacheProperties.getEmptyTtlMinutes());
    }

    public boolean isEmptyValue(Object value) {
        return CacheKeys.EMPTY_VALUE.equals(value);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public void clearBookCaches() {
        delete(CacheKeys.BOOK_LIST);
        delete(CacheKeys.CATEGORY_TREE);
    }

    public void clearBookDetail(Long id) {
        delete(CacheKeys.bookDetail(id));
    }

    private void put(String key, Object value, long minutes) {
        // 为缓存 TTL 增加少量随机抖动，避免大量 key 同时失效。
        long randomSeconds = ThreadLocalRandom.current().nextLong(30, 181);
        redisTemplate.opsForValue().set(key, value, Duration.ofMinutes(minutes).plusSeconds(randomSeconds));
    }
}
