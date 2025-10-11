//package com.dji.sample.configuration;
//
//import com.baomidou.mybatisplus.annotation.DbType;
//import com.baomidou.mybatisplus.core.MybatisConfiguration;
//import com.baomidou.mybatisplus.core.config.GlobalConfig;
//import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
//import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
//import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
//import org.apache.ibatis.logging.stdout.StdOutImpl;
//import org.apache.ibatis.plugin.Interceptor;
//import org.apache.ibatis.session.SqlSessionFactory;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Primary;
//import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
//
//import javax.sql.DataSource;
//@Configuration
//public class MybatisPlusConfig {
//
//    @Bean
//    public MybatisPlusInterceptor mybatisPlusInterceptor() {
//        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
//        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
//        return interceptor;
//    }
//    @Bean(name = "globalConfig")
//    @Primary
//    public GlobalConfig globalConfig() {
//        GlobalConfig globalConfig = new GlobalConfig();
//        return globalConfig;
//    }
//    @Bean("sqlSessionFactory")
//    @Primary
//    public SqlSessionFactory dbSqlSessionFactory(@Qualifier("dataSource") DataSource dataSource, GlobalConfig globalConfig) throws Exception {
//        // MybatisSqlSessionFactoryBean
//        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
//        factoryBean.setDataSource(dataSource);
//        factoryBean.setGlobalConfig( globalConfig());
//        Interceptor[] plugins = new Interceptor[1];
//        plugins[0] = mybatisPlusInterceptor();
//        factoryBean.setPlugins(plugins);
//
//        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
//        // 需要在这里指定xml文件的位置，不然自定义的sql会报Invalid bound statement异常
//        factoryBean.setMapperLocations(resolver.getResources("classpath*:mapper/*Mapper.xml"));
//
//        // 导入mybatis配置 构造方法，解决动态数据源循环依赖问题
//        MybatisConfiguration mybatisConfiguration = new MybatisConfiguration();
//        // 配置打印sql语句
//        mybatisConfiguration.setLogImpl(StdOutImpl.class);
//        factoryBean.setConfiguration(mybatisConfiguration);
//        return factoryBean.getObject();
//    }
//
//}
