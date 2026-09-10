/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.Result
 *  com.bsball.core.GlobalExceptionHandler
 *  com.bsball.exception.BusinessException
 *  com.bsball.exception.UnauthorizedException
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.http.HttpStatus
 *  org.springframework.http.HttpStatusCode
 *  org.springframework.http.ResponseEntity
 *  org.springframework.http.converter.HttpMessageNotReadableException
 *  org.springframework.web.bind.MethodArgumentNotValidException
 *  org.springframework.web.bind.annotation.ExceptionHandler
 *  org.springframework.web.bind.annotation.RestControllerAdvice
 *  org.springframework.web.servlet.resource.NoResourceFoundException
 */
package com.bsball.core;

import com.bsball.common.Result;
import com.bsball.exception.BusinessException;
import com.bsball.exception.UnauthorizedException;
import java.util.stream.Collectors;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(value={UnauthorizedException.class})
    public ResponseEntity<Result<Object>> handleUnauthorized(UnauthorizedException e) {
        log.warn("UnauthorizedException: {}", (Object)e.getMessage());
        return ResponseEntity.status((HttpStatusCode)HttpStatus.UNAUTHORIZED).body((Object)Result.fail((int)401, (String)e.getMessage()));
    }

    @ExceptionHandler(value={BusinessException.class})
    public ResponseEntity<Result<Object>> handleBusiness(BusinessException e) {
        log.warn("BusinessException: {}", (Object)e.getMessage());
        int code = e.getCode();
        HttpStatus status = switch (code) {
            case 401 -> HttpStatus.UNAUTHORIZED;
            case 403 -> HttpStatus.FORBIDDEN;
            case 404 -> HttpStatus.NOT_FOUND;
            case 400 -> HttpStatus.BAD_REQUEST;
            case 429 -> HttpStatus.TOO_MANY_REQUESTS;
            default -> HttpStatus.OK;
        };
        Result r = Result.fail((String)e.getMessage());
        r.setCode(code);
        return ResponseEntity.status((HttpStatusCode)status).body((Object)r);
    }

    @ExceptionHandler(value={NoResourceFoundException.class})
    public ResponseEntity<Result<Object>> handleNoResource(NoResourceFoundException e) {
        log.debug("No resource: {}", (Object)e.getResourcePath());
        return ResponseEntity.status((HttpStatusCode)HttpStatus.NOT_FOUND).body((Object)Result.fail((int)404, (String)("\u63a5\u53e3\u4e0d\u5b58\u5728: " + e.getResourcePath())));
    }

    @ExceptionHandler(value={IllegalArgumentException.class})
    public ResponseEntity<Result<Object>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("IllegalArgumentException: {}", (Object)e.getMessage());
        return ResponseEntity.badRequest().body((Object)Result.fail((int)400, (String)e.getMessage()));
    }

    @ExceptionHandler(value={HttpMessageNotReadableException.class})
    public ResponseEntity<Result<Object>> handleMessageNotReadable(HttpMessageNotReadableException e) {
        Object detail = e.getMessage();
        Throwable cause = e.getMostSpecificCause();
        if (cause != null && cause != e) {
            detail = (String)detail + " \u2192 " + cause.getClass().getSimpleName() + ": " + cause.getMessage();
        }
        log.warn("\u8bf7\u6c42\u4f53\u89e3\u6790\u5931\u8d25: {}", detail);
        return ResponseEntity.badRequest().body((Object)Result.fail((int)400, (String)("\u8bf7\u6c42\u53c2\u6570\u683c\u5f0f\u9519\u8bef\uff1a" + (String)detail)));
    }

    @ExceptionHandler(value={MethodArgumentNotValidException.class})
    public ResponseEntity<Result<Object>> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream().map(err -> err.getField() + ": " + err.getDefaultMessage()).collect(Collectors.joining("; "));
        log.warn("\u53c2\u6570\u6821\u9a8c\u5931\u8d25: {}", (Object)msg);
        return ResponseEntity.badRequest().body((Object)Result.fail((int)400, (String)msg));
    }

    @ExceptionHandler(value={Exception.class})
    public ResponseEntity<Result<Object>> handleException(Exception e) {
        log.error("\u670d\u52a1\u5668\u5185\u90e8\u9519\u8bef", (Throwable)e);
        return ResponseEntity.status((HttpStatusCode)HttpStatus.INTERNAL_SERVER_ERROR).body((Object)Result.fail((String)"\u670d\u52a1\u5668\u7e41\u5fd9\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5"));
    }
}

