package com.lab.study.albummanageservice.exception;

import com.LAB.study.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AlbumExceptionHandler {

    // 处理 UserContextHolder 抛出的鉴权失败异常
    @ExceptionHandler(SecurityException.class)
    public Result<Void> handleSecurityException(SecurityException e, HttpServletRequest request) {
        return Result.error(401, e.getMessage() == null ? "用户未认证" : e.getMessage());
    }

    // 处理 AlbumService 中校验抛出的基础运行时异常
    @ExceptionHandler(RuntimeException.class)
    public Result<Void> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        String msg = e.getMessage();
        return Result.error(500, (msg == null || msg.isBlank()) ? "系统繁忙，请稍后重试" : msg);
    }

    // 兜底所有未知系统异常
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        e.printStackTrace();
        return Result.error(500, "服务器内部错误");
    }
}
