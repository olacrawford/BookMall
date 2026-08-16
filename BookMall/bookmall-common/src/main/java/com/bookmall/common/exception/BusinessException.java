package com.bookmall.common.exception;

import com.bookmall.common.constant.ErrorCode;
import lombok.Getter;

//业务层主动抛出异常
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

}