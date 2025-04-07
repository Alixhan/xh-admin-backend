package com.xh.system.client.configuration;

import com.xh.system.client.exchange.SysFileClient;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.time.Duration;

/**
 * 系统服务 web client，跨服务调用配置
 * sunxh 2023/9/22
 */
public class SystemWebClientConfig {

    @Resource
    private WebClient.Builder myWebClientBuilder;

    //服务名或者ip地址调用对应服务
    @Value("${service.system:lb://system}")
    private String baseUrl;

    public HttpServiceProxyFactory httpServiceProxyFactory() {
        WebClientAdapter webClientAdapter = WebClientAdapter.create(myWebClientBuilder.baseUrl(baseUrl).build());
        // 设置超时时间10s
        webClientAdapter.setBlockTimeout(Duration.ofSeconds(10));
        return HttpServiceProxyFactory.builderFor(webClientAdapter).build();
    }

    /**
     * SysFileServiceClient
     */
    @Bean
    public SysFileClient getSysFileServiceClient() {
        return httpServiceProxyFactory().createClient(SysFileClient.class);
    }
}
