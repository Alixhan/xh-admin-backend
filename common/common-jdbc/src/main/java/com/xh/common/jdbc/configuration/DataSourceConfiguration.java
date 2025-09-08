package com.xh.common.jdbc.configuration;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 数据源配置
 * 参考文档 <a href="https://docs.spring.io/spring-boot/docs/3.0.5/reference/htmlsingle/#howto.data-access.configure-custom-datasource">...</a>
 * sunxh 2023/4/17
 */
@Configuration(proxyBeanMethods = false)
public class DataSourceConfiguration {

    /**
     * 第一数据源配置信息
     */
    @Bean("firstDataSourceProperties")
    @ConfigurationProperties("spring.datasource.first")
    public DataSourceProperties firstDataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * 第一数据源
     */
    @Primary
    @Bean("firstDataSource")
    @ConfigurationProperties("spring.datasource.first.configuration")
    public DataSource firstDataSource(@Qualifier("firstDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    /**
     * 第一数据源JdbcTemplate
     */
    @Primary
    @Bean("firstJdbcTemplate")
    public JdbcTemplate firstJdbcTemplate(@Qualifier("firstDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * 第二数据源配置信息
     */
    @Bean("secondDataSourceProperties")
    @ConfigurationProperties("spring.datasource.second")
    public DataSourceProperties secondDataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * 第二数据源
     */
    @Bean("secondDataSource")
    @ConfigurationProperties("spring.datasource.second.configuration")
    public DataSource secondDataSource(@Qualifier("secondDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    /**
     * 第二数据源JdbcTemplate
     */
    @Bean("secondJdbcTemplate")
    public JdbcTemplate secondJdbcTemplate(@Qualifier("secondDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
