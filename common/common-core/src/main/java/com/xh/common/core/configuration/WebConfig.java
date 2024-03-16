package com.xh.common.core.configuration;

import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.exception.NotWebContextException;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.xh.common.core.Constant;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Resource;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * web配置类
 * sunxh 2023/2/26
 */
@Configuration(proxyBeanMethods = false)
@EnableWebMvc
@ConfigurationProperties("spring.web")
public class WebConfig implements WebMvcConfigurer {

    @Resource
    private MyLoggerInterceptor myLoggerInterceptor;
    @Resource
    private SaTokenConfig saTokenConfig;
    @Setter
    private String [] allowedOriginPatterns;



    /**
     * 资源跨域设置
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOriginPatterns)
                .allowedMethods("POST", "PUT", "GET", "OPTIONS", "DELETE")
                .maxAge(3600);
    }

    /**
     * 请求拦截器
     */
    @Override
    public void addInterceptors(@Nonnull InterceptorRegistry registry) {
        WebMvcConfigurer.super.addInterceptors(registry);
        myLoggerInterceptor.addInterceptor(registry);
        // 注册 Sa-Token 拦截器，打开注解式鉴权功能
        registry.addInterceptor(
                        new SaInterceptor(handle -> {
                            if (handle instanceof HandlerMethod) {
                                StpUtil.checkLogin();
                                //续签token过期时间
                                StpUtil.renewTimeout(saTokenConfig.getTimeout());
                            }
                        })
                )
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/swagger-ui.html",
                        "/swagger-ui.html/**",
                        "/swagger-ui/**",
                        "/v3/**"
                ).order(5);
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 在头部添加，优先级别最高
        converters.add(0, new MappingJackson2HttpMessageConverter(getDefaultObjectMapper()));
    }


    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    /**
     * 定义全局默认时间序列化
     */
    public static ObjectMapper getDefaultObjectMapper() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder()
                .indentOutput(true)
                .dateFormat(new SimpleDateFormat("yyyy-MM-dd"))
                .simpleDateFormat(DATE_TIME_FORMAT)
                .serializers(new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)))
                .serializers(new LocalDateSerializer(DateTimeFormatter.ofPattern(DATE_FORMAT)))
                .deserializers(new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)))
                .deserializers(new LocalDateDeserializer(DateTimeFormatter.ofPattern(DATE_FORMAT)))
                .modulesToInstall(new ParameterNamesModule());
        return builder.build();
    }

    /**
     * 通用跨服务用WebClientBuilder，携带鉴权token，设置序列化内容，默认启用负载均衡
     */
    @Bean(name = "myWebClientBuilder")
    @Scope("prototype")
    @LoadBalanced
    WebClient.Builder getMyWebClientBuilder() {
        return WebClient.builder()
                //过滤器重构请求，携带鉴权token
                .filter((request, next) -> {
                    final ClientRequest.Builder req = ClientRequest.from(request);
                    try {
                        req.header(StpUtil.getTokenName(), StpUtil.getTokenValue());
                    } catch (NotWebContextException e) {
                        //非web环境则传输一个固定字符串，标识自动程序执行跨服务
                        req.header(Constant.AUTO_FEIGN_KEY, "自动程序跨服务");
                    }
                    return next.exchange(req.build());
                })
                //序列化配置
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs().jackson2JsonDecoder(
                                new Jackson2JsonDecoder(WebConfig.getDefaultObjectMapper())
                        )).build()
                );
    }
}
