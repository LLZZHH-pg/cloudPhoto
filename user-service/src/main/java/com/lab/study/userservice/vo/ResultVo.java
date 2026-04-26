package com.lab.study.userservice.vo;

import lombok.Data;

@Data
public class ResultVo<T> {
    private Integer code;
    private String message;
    private T data;

    // 成功返回
    public static <T> ResultVo<T> success(T data) {
        ResultVo<T> resultVo = new ResultVo<>();
        resultVo.setCode(200);
        resultVo.setMessage("操作成功");
        resultVo.setData(data);
        return resultVo;
    }

    // 失败返回
    public static <T> ResultVo<T> fail(String message) {
        ResultVo<T> resultVo = new ResultVo<>();
        resultVo.setCode(500);
        resultVo.setMessage(message);
        return resultVo;
    }
}