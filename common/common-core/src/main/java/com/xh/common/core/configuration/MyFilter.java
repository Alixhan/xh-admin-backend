package com.xh.common.core.configuration;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.router.SaRouter;
import com.alibaba.fastjson2.JSONObject;
import com.xh.common.core.entity.SysLog;
import com.xh.common.core.service.CommonService;
import com.xh.common.core.utils.CommonUtil;
import com.xh.common.core.web.MyContext;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * web过滤器
 *
 * @author sunxh 2024/5/8
 */
@Configuration
@Slf4j
public class MyFilter extends HttpFilter {

    @Resource
    private CommonService commonService;

    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException {
        preHandle(request);
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(requestWrapper, responseWrapper);
        } catch (Exception e) {
            log.error("请求异常", e);
            MyContext.getSysLog().setStackTrace(CommonUtil.getThrowString(e));
        }
        afterHandle(requestWrapper, responseWrapper);
    }

    /**
     * 前置处理
     */
    private void preHandle(HttpServletRequest request) {
        SysLog sysLog = MyContext.getSysLog(true);
        sysLog.setStartTime(LocalDateTime.now());
        sysLog.setMethod(request.getMethod());
        sysLog.setUrl(request.getRequestURI());
        sysLog.setContentType(request.getContentType());
        String ip = request.getHeader("X-Real-IP");
        if (CommonUtil.isEmpty(ip)) {
            ip = request.getRemoteAddr();
        }
        sysLog.setIp(ip);
        String ipRegion2 = CommonUtil.getIpRegion2(ip);
        sysLog.setIpAddress(ipRegion2);

        Map<String, String[]> parameterMap = request.getParameterMap();
        if (!parameterMap.isEmpty()) {
            JSONObject parameter = JSONObject.from(parameterMap);
            sysLog.setRequestParameter(parameter.toString());
        }
    }

    /**
     * 后置处理
     */
    private void afterHandle(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response) throws IOException {
        SysLog sysLog = MyContext.getSysLog();
        ServletInputStream inputStream = request.getRequest().getInputStream();
        //如果文件流已读取则从缓存中获取请求体
        if (inputStream.isFinished()) {
            sysLog.setRequestBody(request.getContentAsString());
        } else {
            // 否则直接从request中获取请求体
            String requestBody = new String(inputStream.readAllBytes());
            sysLog.setRequestBody(requestBody);
        }
        //存储响应体内容
        String contentType = response.getContentType();
        if (contentType != null && contentType.startsWith("application/json")) {
            byte[] contentAsByteArray = response.getContentAsByteArray();
            String responseContent = new String(contentAsByteArray);
            sysLog.setResponseBody(responseContent);
        }
        response.copyBodyToResponse();

        boolean hit = SaRouter
                .notMatch(ignored -> request.getRequestURI().endsWith("/query"))
                .notMatch(
                        "/api/system/log/get/**",
                        "/api/system/user/queryOnlineUser",
                        "/api/file/operation/download",
                        "/api/system/user/queryUserGroupList"
                )
                .notMatchMethod("OPTIONS")
                .isHit();
        //设置指定匹配的或者出现报错的才记录日志
        if (hit || sysLog.getStackTrace() != null) {
            final SaTokenContext saTokenContext = SaManager.getSaTokenContext();
            //异步存储请求的日志信息
            Mono.just(sysLog).subscribe(i -> {
                // 因为会开启新线程，所以把上SaToken下文对象传递进来
                if (saTokenContext != null) {
                    SaManager.setSaTokenContext(saTokenContext);
                }
                commonService.saveSysLog(i);
            });
        }
    }
}
