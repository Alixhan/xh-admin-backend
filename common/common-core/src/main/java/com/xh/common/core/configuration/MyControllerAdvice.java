package com.xh.common.core.configuration;

import cn.dev33.satoken.exception.SaTokenException;
import com.xh.common.core.utils.CommonUtil;
import com.xh.common.core.web.MyContext;
import com.xh.common.core.web.MyException;
import com.xh.common.core.web.RestResponse;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 控制层异常统一处理
 * sunxh 2023/2/26
 */
@Slf4j
@RestControllerAdvice
public class MyControllerAdvice {

    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(MyException.class)
    public RestResponse<?> handleControllerException(MyException ex) {
        log.info("{} {}", ex.getStackTrace()[0].toString(), ex.getMessage());
        RestResponse<String> response = RestResponse.error(ex.getMessage());
        response.setHttpCode(HttpStatus.OK.value());
        return response;
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(SaTokenException.class)
    public RestResponse<?> handlerSaTokenException(SaTokenException e) {
        RestResponse<?> res = RestResponse.error("服务器繁忙，请稍后重试...");
        // 根据不同异常细分状态码返回不同的提示
        if (e.getCode() == 11001) {
            res.setMessage("用户未登录!");
            res.setHttpCode(HttpStatus.UNAUTHORIZED.value());
        }
        if (e.getCode() == 11011) {
            res.setMessage("未读到有效的token!");
            res.setHttpCode(HttpStatus.UNAUTHORIZED.value());
        }
        if (e.getCode() == 11012) {
            res.setHttpCode(HttpStatus.UNAUTHORIZED.value());
            res.setMessage("登录状态已过期，请重新登录!");
        }
        if (e.getCode() == 11014) {
            res.setHttpCode(HttpStatus.UNAUTHORIZED.value());
            res.setMessage("用户在其他地方登录，已被顶下线！");
        }
        if (e.getCode() == 11015) {
            res.setHttpCode(HttpStatus.UNAUTHORIZED.value());
            res.setMessage("用户已在其他地方登录，或者被管理员踢下线！");
        }
        if (e.getCode() == 11016) {
            res.setMessage("Token已被冻结！");
        }
        if (e.getCode() == 11041) {
            res.setHttpCode(HttpStatus.FORBIDDEN.value());
            res.setMessage("角色无权操作！");
        }
        if (e.getCode() == 11051) {
            res.setHttpCode(HttpStatus.FORBIDDEN.value());
            res.setMessage("权限不足，无法操作！");
        }
        return res;
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public RestResponse<?> handleControllerException(HttpServletRequest request, Throwable e) {
        log.error("服务器运行异常", e);
        MyContext.getSysLog().setStackTrace(CommonUtil.getThrowString(e));
        RestResponse<?> response = RestResponse.error("服务器运行异常！");
        HttpStatus status = getStatus(request);
        if (status != null) {
            response.setHttpCode(status.value());
        }
        return response;
    }

    private HttpStatus getStatus(HttpServletRequest request) {
        Integer code = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (code != null) return HttpStatus.resolve(code);
        return null;
    }
}
