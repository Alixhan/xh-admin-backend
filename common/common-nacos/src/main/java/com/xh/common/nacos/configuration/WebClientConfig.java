package com.xh.common.nacos.configuration;

import cn.dev33.satoken.exception.NotWebContextException;
import cn.dev33.satoken.stp.StpUtil;
import com.xh.common.core.Constant;
import com.xh.common.core.configuration.WebConfig;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient跨服务配置
 * sunxh 2023/2/26
 */
@Configuration(proxyBeanMethods = false)
public class WebClientConfig {
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
                        .codecs(configurer -> configurer.defaultCodecs().jacksonJsonDecoder(
                                new JacksonJsonDecoder(WebConfig.getDefaultJsonMapper())
                        )).build()
                );
    }
}
