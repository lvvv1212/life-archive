package com.lifearchive.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public Result<Void> handleRuntimeException(RuntimeException e) {
        log.error("业务异常: {}", e.getMessage());
        return Result.error(e.getMessage());
    }

    /** JSON 请求体解析失败（如日期格式不匹配、字段类型错误等） */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        String msg = "请求数据格式异常";
        Throwable cause = e.getCause();
        if (cause != null) {
            msg = cause.getMessage();
            // 截断过长的异常信息
            if (msg.length() > 120) msg = msg.substring(0, 120) + "...";
        }
        log.error("请求解析失败: {}", msg);
        return Result.error(msg);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常: ", e);
        return Result.error("服务器内部错误: " + e.getClass().getSimpleName());
    }
}
