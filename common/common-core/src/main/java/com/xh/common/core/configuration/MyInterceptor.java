package com.xh.common.core.configuration;

import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import com.xh.common.core.Constant;
import com.xh.common.core.dto.SysLoginUserInfoDTO;
import com.xh.common.core.dto.SysUserDTO;
import com.xh.common.core.entity.SysLog;
import com.xh.common.core.utils.CommonUtil;
import com.xh.common.core.utils.LoginUtil;
import com.xh.common.core.web.MyContext;
import com.xh.common.core.web.MyException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

/**
 * 拦截器，实现SaToken鉴权
 *
 * @author sunxh 2023/2/26
 */
@Configuration
@Slf4j
public class MyInterceptor extends SaInterceptor {

    @Resource
    private SaTokenConfig saTokenConfig;

    @Override
    public boolean preHandle(HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull Object handler) throws Exception {
        //自动程序跨服务，无法获取登录用户，直接登录为自动程序用户
        String feignValue = request.getHeader(Constant.AUTO_FEIGN_KEY);
        if (CommonUtil.isNotEmpty(feignValue)) {
            log.info("自动程序跨服务：{}", feignValue);
            StpUtil.login(feignValue, "auto-job");
        }

        //对于无法添加header但需要鉴权的请求可以将token放在参数中，手动从请求中获取token设置
        String tokenValue = request.getParameter(StpUtil.getTokenName());
        if (CommonUtil.isNotEmpty(tokenValue)) {
            StpUtil.setTokenValue(tokenValue);
        }

        if (handler instanceof HandlerMethod handlerMethod) {
            final var requestURI = request.getRequestURI();
            SysLog sysLog = MyContext.getSysLog();
            Class<?> controllerClass = handlerMethod.getBeanType();
            Tag tag = controllerClass.getAnnotation(Tag.class);
            Method method = handlerMethod.getMethod();
            Operation operation = method.getAnnotation(Operation.class);
            if (tag == null || CommonUtil.isEmpty(tag.name())) {
                throw new MyException("保持良好的开发规范，请补充：%s 类Tag注解name属性，描述controller用途".formatted(controllerClass.getName()));
            }
            if (operation == null || CommonUtil.isEmpty(operation.description())) {
                throw new MyException("保持良好的开发规范，请补充：%s 方法Operation注解description属性描述方法用途".formatted(method.getName()));
            }

            sysLog.setTag(tag.name());
            sysLog.setOperation(operation.description());

            //打印一下控制器相关日志
            log.info("{} {} {}--{}", controllerClass.getName(), method.getName(), tag.name(), operation.description());

            this.auth = ignored -> {
                // SaToken鉴权
                StpUtil.checkLogin();
                SysLoginUserInfoDTO userInfoDTO = LoginUtil.getSysUserInfo();
                SysUserDTO user = userInfoDTO.getUser();

                // 演示站的演示账号部分操作不允许
                if(Boolean.TRUE.equals(user.getIsDemo())) {
                    boolean hit = SaRouter.notMatch(
                                    "/api/system/user/personalCenterSave",
                                    "/api/file/operation/upload",
                                    "/api/system/user/imports",
                                    "/api/system/user/resetPassword",
                                    "/api/system/user/saveUserJobs",
                                    "/api/system/user/saveUserGroup",
                                    "/api/system/user/delUserGroup"
                            )
                            .notMatch(obj -> {
                                boolean isDel = requestURI.endsWith("/del");
                                boolean isSave = requestURI.endsWith("/save");
                                boolean isSwitchProp = requestURI.endsWith("/switch_prop");
                                return isDel || isSave || isSwitchProp;
                            }).isHit();
                    // 代码生成器不拦截
                    boolean hitGenCode = SaRouter.match("/api/generator/**").isHit();
                    if(!hit && !hitGenCode) throw new MyException("演示账号不允许此操作");
                }

                if (Boolean.TRUE.equals(user.getAutoRenewal())) {
                    //续签token过期时间
                    StpUtil.renewTimeout(saTokenConfig.getTimeout());
                }
            };
            return super.preHandle(request, response, handler);
        }
        return true;
    }
}
