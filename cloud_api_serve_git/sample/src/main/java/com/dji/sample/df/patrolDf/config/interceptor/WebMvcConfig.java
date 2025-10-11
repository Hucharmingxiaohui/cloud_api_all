package com.dji.sample.df.patrolDf.config.interceptor;

import com.df.framework.config.FileConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

/**
 * MVC配置
 *
 * @author lyc
 **/
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private static final String ORIGINS[] = new String[]{"GET", "POST", "PUT", "DELETE", "OPTIONS"};

    @Autowired
    private FileConfig fileConfig;
    @Autowired
    private AuthorizationInterceptor authorizationInterceptor;

    /**
     * 单独对某类请求过滤不验证。主要包含静态文件、以及无法指定具体的Controller方法的地址，效果同@NoToken注解
     */
    private static final List<String> EXCLUDE_PATH = Arrays.asList(
            "/images/**"
    );

    /**
     * 静态文件访问配置
     *
     * @param registry
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        //本地文件映射路径
        registry.addResourceHandler("/dog/images/**").addResourceLocations("file:" + fileConfig.getFileSavePath() + "/");
    }

    /**
     * 自定义拦截规则
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //设置拦截器顺序
        //token验证 登录前的访问不拦截
        /*registry.addInterceptor(authorizationInterceptor).addPathPatterns("/**")
                .excludePathPatterns(Constants.EXCLUDE_PATH).order(0);*/
        //IP异常验证
        /*registry.addInterceptor(onlineRequestIPInterceptor).addPathPatterns("/**")
                .excludePathPatterns(Constants.EXCLUDE_PATH).order(1);*/
        //接口权限验证
        /*registry.addInterceptor(defRequiresPermissionsInterceptor).addPathPatterns("/**")
                .excludePathPatterns(Constants.EXCLUDE_PATH).order(2);*/
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 所有的当前站点的请求地址，都支持跨域访问。
                .allowedOriginPatterns("*") // 所有的外部域都可跨域访问。 如果是localhost则很难配置，因为在跨域请求的时候，外部域的解析可能是localhost、127.0.0.1、主机名
                .allowCredentials(true) // 是否支持跨域用户凭证
                .allowedMethods(ORIGINS) // 当前站点支持的跨域请求类型是什么
                .maxAge(36000); // 超时时长设置为1小时。 时间单位是秒。
    }
}
