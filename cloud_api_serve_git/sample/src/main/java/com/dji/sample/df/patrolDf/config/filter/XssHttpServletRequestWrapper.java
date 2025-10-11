package com.dji.sample.df.patrolDf.config.filter;

import com.df.framework.xss.EnCodeXSS;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.StreamUtils;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Map;
import java.util.Vector;

/**
 * XSS过滤处理
 *
 * @author ruoyi
 * <p>
 * 姜学云 2022/3/9 15:09 移植e2100国网测试时的修改
 */
public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {
    HttpServletRequest orgRequest = null;
    private final Map<String, String[]> parameterMap;
    private byte[] body; //用于保存读取body中数据

    public XssHttpServletRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        orgRequest = request;
        parameterMap = request.getParameterMap();
        body = StreamUtils.copyToByteArray(request.getInputStream());
    }

    // 重写几个HttpServletRequestWrapper中的方法

    /**
     * 获取所有参数名
     *
     * @return 返回所有参数名
     */
    @Override
    public Enumeration<String> getParameterNames() {
        Vector<String> vector = new Vector<String>(parameterMap.keySet());
        return vector.elements();
    }

    /**
     * 覆盖getParameter方法，将参数名和参数值都做xss & sql过滤。<br/>
     * 如果需要获得原始的值，则通过super.getParameterValues(name)来获取<br/>
     * getParameterNames,getParameterValues和getParameterMap也可能需要覆盖
     */
    @Override
    public String getParameter(String name) {
        String[] results = parameterMap.get(name);
        if (results == null || results.length <= 0) {
            return null;
        } else {
            String value = results[0];
            if (value != null) {
                value = EnCodeXSS.xssEncode(value);
            }
            return value;
        }
    }

    /**
     * 获取指定参数名的所有值的数组，如：checkbox的所有数据 接收数组变量 ，如checkobx类型
     */
    @Override
    public String[] getParameterValues(String name) {
        String[] results = parameterMap.get(name);
        if (results == null || results.length <= 0) {
            return null;
        } else {
            int length = results.length;
            for (int i = 0; i < length; i++) {
                results[i] = EnCodeXSS.xssEncode(results[i]);
            }
            return results;
        }
    }

    /**
     * 覆盖getHeader方法，将参数名和参数值都做xss & sql过滤。<br/>
     * 如果需要获得原始的值，则通过super.getHeaders(name)来获取<br/>
     * getHeaderNames 也可能需要覆盖
     */
    @Override
    public String getHeader(String name) {

        String value = super.getHeader(EnCodeXSS.xssEncode(name));
        if (value != null) {
            value = EnCodeXSS.xssEncode(value);
        }
        return value;
    }

    /**
     * 获取最原始的request
     *
     * @return
     */
    public HttpServletRequest getOrgRequest() {
        return orgRequest;
    }

    /**
     * 获取最原始的request的静态方法
     *
     * @return
     */
    public static HttpServletRequest getOrgRequest(HttpServletRequest req) {
        if (req instanceof XssHttpServletRequestWrapper) {
            return ((XssHttpServletRequestWrapper) req).getOrgRequest();
        }

        return req;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        String s = new String(body);
        if (StringUtils.isNotBlank(s)) {
            s = EnCodeXSS.xssEncode(s);
            body = s.getBytes(StandardCharsets.UTF_8);
        }
        final ByteArrayInputStream bais = new ByteArrayInputStream(body);
        return new ServletInputStream() {

            @Override
            public int read() throws IOException {
                return bais.read();
            }

            @Override
            public boolean isFinished() {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public boolean isReady() {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public void setReadListener(ReadListener arg0) {
                // TODO Auto-generated method stub

            }
        };
    }
}
