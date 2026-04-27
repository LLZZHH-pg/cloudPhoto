package com.lab.study.photomanageservice.vo;

import lombok.Data;

@Data
public class ResultVo<T> {
    private Integer code;
    private String message;
    private T data;

    public static <T> ResultVo<T> success(T data) {
        ResultVo<T> result = new ResultVo<>();
        result.setCode(200);
        result.setMessage("Success");
        result.setData(data);
        return result;
    }

    public static <T> ResultVo<T> success() {
        return success(null);
    }

    public static <T> ResultVo<T> fail(Integer code, String message) {
        ResultVo<T> result = new ResultVo<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}