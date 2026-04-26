package com.xh.common.core.configuration;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Resource;
import jakarta.servlet.Filter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * web配置类
 * sunxh 2023/2/26
 */
@Configuration(proxyBeanMethods = false)
@EnableWebMvc
@ConfigurationProperties("spring.web")
public class WebConfig implements WebMvcConfigurer {

    @Resource
    private MyFilter myFilter;
    @Resource
    private MyInterceptor myInterceptor;
    @Setter
    private String[] allowedOriginPatterns;


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
     * 过滤器配置
     */
    @Bean
    public FilterRegistrationBean<Filter> filterRegistrationBean() {
        FilterRegistrationBean<Filter> filterFilterRegistrationBean = new FilterRegistrationBean<>();
        filterFilterRegistrationBean.setFilter(myFilter);
        filterFilterRegistrationBean.setOrder(1);//执行的顺序
        filterFilterRegistrationBean.addUrlPatterns("/*");
        return filterFilterRegistrationBean;
    }

    /**
     * 请求拦截器
     */
    @Override
    public void addInterceptors(@Nonnull InterceptorRegistry registry) {
        WebMvcConfigurer.super.addInterceptors(registry);
        registry.addInterceptor(myInterceptor)
                .addPathPatterns("/**")
                // 排除swagger文档
                .excludePathPatterns(
                        "/swagger-ui.html",
                        "/swagger-ui.html/**",
                        "/swagger-ui/**",
                        "/v3/**"
                );
    }

    @Override
    public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
        // 在头部添加，优先级别最高
        builder.configureMessageConvertersList(converters -> {
            converters.addFirst(new JacksonJsonHttpMessageConverter(getDefaultJsonMapper()));
            converters.addFirst(new ByteArrayHttpMessageConverter());
        });
    }


    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    /**
     * 定义全局默认映射器
     */
    public static JsonMapper getDefaultJsonMapper() {
        SimpleModule timeModule = new SimpleModule();
        timeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)));
        timeModule.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern(DATE_FORMAT)));
        timeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)));
        timeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern(DATE_FORMAT)));

        return JsonMapper.builder()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .defaultDateFormat(new SimpleDateFormat(DATE_TIME_FORMAT))
                .addModule(timeModule)
                .build();
    }
}