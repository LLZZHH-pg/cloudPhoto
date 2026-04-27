package com.lab.study.photomanageservice.exception;

import com.lab.study.photomanageservice.vo.ResultVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class PhotoExceptionHandler  {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResultVo<Void> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e, HttpServletRequest request) {
        return ResultVo.fail(413, "上传文件过大，超过系统限制");
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResultVo<Void> handleBadRequest(Exception e, HttpServletRequest request) {
        return ResultVo.fail(400, "请求参数错误");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResultVo<Void> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        String msg = e.getMessage();
        return ResultVo.fail(400, (msg == null || msg.isBlank()) ? "请求参数错误" : msg);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResultVo<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        return ResultVo.fail(405, "请求方法不支持");
    }

    @ExceptionHandler(SecurityException.class)
    public ResultVo<Void> handleSecurityException(SecurityException e, HttpServletRequest request) {
        return ResultVo.fail(401, e.getMessage() == null ? "用户未认证" : e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResultVo<Void> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        String msg = e.getMessage();
        return ResultVo.fail(500, (msg == null || msg.isBlank()) ? "系统繁忙，请稍后重试" : msg);
    }

    @ExceptionHandler(Exception.class)
    public ResultVo<Void> handleException(Exception e, HttpServletRequest request) {
        return ResultVo.fail(500, "服务器内部错误");
    }
}
