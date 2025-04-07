package com.xh.common.core.web;

/**
 * 2021-09-26 sunxh
 * 业务异常，用于service抛出业务错误信息
 */
public class MyException extends RuntimeException {

    public MyException(String message) {
        super(message);
    }

    public MyException(Throwable e) {
        super(e);
    }
}
