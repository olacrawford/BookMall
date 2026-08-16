package com.bookmall.book.exception;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.bookmall.common.exception.BusinessException;

public class SentinelBlockedException extends BusinessException {

    public SentinelBlockedException(String message, BlockException cause) {
        super(429, message);
        initCause(cause);
    }
}
