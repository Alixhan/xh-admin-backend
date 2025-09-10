package com.xh.common.core.configuration;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.exception.SaTokenException;
import com.xh.common.core.utils.CommonUtil;
import com.xh.common.core.web.MyContext;
import com.xh.common.core.web.MyException;
import com.xh.common.core.web.NoDataPermissionException;
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
    public RestResponse<?> handleNoDataPermissionException(SaTokenException e) {
        RestResponse<?> res = new RestResponse<>();
        switch (e) {
            case NotLoginException ex -> {
                var message = switch (ex.getType()) {
                    case "-3" -> "登录已超时！";
                    case "-4" -> "账号已在其他地方登录！";
                    case "-5" -> "当前会话已被管理员踢下线！";
                    case "-6" -> "token被冻结！";
                    case "-7" -> "非法格式token！";
                    default -> "会话未登录！";
                };
                res.setMessage(message);
                res.setHttpCode(HttpStatus.UNAUTHORIZED.value());
            }
            case NotRoleException ignored -> {
                res.setHttpCode(HttpStatus.FORBIDDEN.value());
                res.setMessage("角色无权操作！");
            }
            case NotPermissionException ignored -> {
                res.setHttpCode(HttpStatus.FORBIDDEN.value());
                res.setMessage("权限不足，无法操作！");
            }
            default -> res.setMessage("服务器繁忙，请稍后重试...");
        }
        return res;
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(NoDataPermissionException.class)
    public RestResponse<?> handleNoDataPermissionException(NoDataPermissionException e) {
        log.error("越权访问数据", e);
        MyContext.getSysLog().setStackTrace(CommonUtil.getThrowString(e));
        return RestResponse.error("越权访问数据！");
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
