package com.dji.sample.df.patrolDf.config.filter;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 防止XSS攻击的过滤器
 *
 * @author ruoyi
 * <p>
 * 姜学云 2022/3/9 15:09 移植e2100国网测试时的修改
 */
public class XssFilter implements Filter {
    /**
     * 排除链接
     */
    public List<String> excludes = new ArrayList<>();

    /**
     * xss过滤开关
     */
    public boolean enabled = false;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        String tempExcludes = filterConfig.getInitParameter("excludes");
        String tempEnabled = filterConfig.getInitParameter("enabled");
        if (StringUtils.isNotEmpty(tempExcludes)) {
            String[] url = tempExcludes.split(",");
            Collections.addAll(excludes, url);
        }
        if (StringUtils.isNotEmpty(tempEnabled)) {
            enabled = Boolean.parseBoolean(tempEnabled);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
//        if (handleExcludeURL(req, resp)) {
//            chain.doFilter(request, resp);
//            return;
//        }
        XssHttpServletRequestWrapper xssRequest = new XssHttpServletRequestWrapper(req);
        resp.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
        resp.setHeader(HttpHeaders.PRAGMA, "no-cache");
        //resp.setHeader("X-XSS-Protection", "1");
        //resp.setHeader("X-Frame-Options", "SAMEORIGIN");
        resp.setDateHeader(HttpHeaders.EXPIRES, 0);
        chain.doFilter(xssRequest, resp);

        //多产生过滤器

        //System.out.println("自定义过滤器XssFilter触发,拦截url:"+((HttpServletRequest) request).getRequestURI());
    }

    private boolean handleExcludeURL(HttpServletRequest request, HttpServletResponse response) {
        if (!enabled) {
            //      return true;
        }
//        if (excludes == null || excludes.isEmpty()) {
//            return false;
//        }
        String url = request.getServletPath();
        for (String pattern : excludes) {
            Pattern p = Pattern.compile("^" + pattern);
            Matcher m = p.matcher(url);
            if (m.find()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void destroy() {

    }
}
