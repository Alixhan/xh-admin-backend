package com.xh.common.core.configuration;

import com.xh.common.core.dao.BaseJdbcDao;
import com.xh.common.core.dao.BaseJdbcDaoImpl;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 数据源配置
 * 该配置当前作为共用模块引入，这样所有引入该模块的微服务数据源均配置一致
 * 如果需要给各个微服务配置不同数据源，将此配置在common-core模块删除，并单独在具体服务中配置
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
     * 第一数据源 JdbcTemplate
     */
    @Primary
    @Bean("firstJdbcTemplate")
    public JdbcTemplate firstJdbcTemplate(@Qualifier("firstDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * 第一数据源 BaseJdbcDao
     */
    @Primary
    @Bean("firstBaseJdbcDao")
    public BaseJdbcDao firstBaseJdbcDao(@Qualifier("firstJdbcTemplate") JdbcTemplate jdbcTemplate) {
        return new BaseJdbcDaoImpl(jdbcTemplate);
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
     * 第二数据源 JdbcTemplate
     */
    @Bean("secondJdbcTemplate")
    public JdbcTemplate secondJdbcTemplate(@Qualifier("secondDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * 第二数据源 BaseJdbcDao
     */
    @Bean("secondBaseJdbcDao")
    public BaseJdbcDao secondBaseJdbcDao(@Qualifier("secondJdbcTemplate") JdbcTemplate jdbcTemplate) {
        return new BaseJdbcDaoImpl(jdbcTemplate);
    }
}
