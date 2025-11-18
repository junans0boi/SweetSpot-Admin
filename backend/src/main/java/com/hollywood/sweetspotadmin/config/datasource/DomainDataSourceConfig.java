package com.hollywood.sweetspotadmin.config.datasource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate; // ✅ [추가]
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.hollywood.sweetspotadmin.place.repository", entityManagerFactoryRef = "domainEntityManagerFactory", transactionManagerRef = "domainTransactionManager")
public class DomainDataSourceConfig {

    @Bean(name = "domainProperties")
    @ConfigurationProperties(prefix = "datasources.domain")
    public DataSourceProperties domainDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "domainJpaProperties")
    @ConfigurationProperties(prefix = "datasources.domain.jpa")
    public JpaProperties domainJpaProperties() {
        return new JpaProperties();
    }

    @Bean(name = "domainDataSource")
    public DataSource domainDataSource(@Qualifier("domainProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean(name = "domainEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean domainEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("domainDataSource") DataSource dataSource,
            @Qualifier("domainJpaProperties") JpaProperties jpaProps) {
        return builder
                .dataSource(dataSource)
                .packages("com.hollywood.sweetspotadmin.place.model")
                .persistenceUnit("domain")
                .properties(jpaProps.getProperties())
                .build();
    }

    @Bean(name = "domainTransactionManager")
    public PlatformTransactionManager domainTransactionManager(
            @Qualifier("domainEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory.getObject());
    }

    /**
     * ✅ [신규] 'domain' (PostgreSQL) 전용 JdbcTemplate Bean 추가
     */
    @Bean(name = "domainJdbcTemplate")
    public JdbcTemplate domainJdbcTemplate(@Qualifier("domainDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}