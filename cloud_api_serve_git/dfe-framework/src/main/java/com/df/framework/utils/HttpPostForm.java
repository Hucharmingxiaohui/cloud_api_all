package com.df.framework.utils;


import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSessionContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class HttpPostForm {
    private HttpURLConnection httpConn;
    private Map<String, Object> queryParams;
    private String charset;

    /**
     * 构造方法
     *
     * @param requestURL  请求地址
     * @param charset     请求的编码
     * @param headers     请求头
     * @param queryParams 请求字段
     * @throws IOException
     */
    public HttpPostForm(String requestURL, String charset, Map<String, String> headers, Map<String, Object> queryParams) throws IOException {
        this.charset = charset;
        if (queryParams == null) {
            this.queryParams = new HashMap<>();
        } else {
            this.queryParams = queryParams;
        }
        //  直接通过主机认证
        try {
            HostnameVerifier hv = (urlHostName, session) -> true;
            //  配置认证管理器
            javax.net.ssl.TrustManager[] trustAllCerts = {new TrustAllTrustManager()};
            SSLContext sc = SSLContext.getInstance("SSL");
            SSLSessionContext sslsc = sc.getServerSessionContext();
            sslsc.setSessionTimeout(0);
            sc.init(null, trustAllCerts, null);
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            //  激活主机认证
            HttpsURLConnection.setDefaultHostnameVerifier(hv);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
        URL url = new URL(requestURL);
        httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setUseCaches(false);
        httpConn.setDoOutput(true);    // 表明是post请求
        httpConn.setDoInput(true);
        httpConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        if (headers != null && headers.size() > 0) {
            Iterator<String> it = headers.keySet().iterator();
            while (it.hasNext()) {
                String key = it.next();
                String value = headers.get(key);
                httpConn.setRequestProperty(key, value);
            }
        }
    }

    public HttpPostForm(String requestURL, String charset, Map<String, String> headers) throws IOException {
        this(requestURL, charset, headers, null);
    }

    public HttpPostForm(String requestURL, String charset) throws IOException {
        this(requestURL, charset, null, null);
    }

    /**
     * 添加参数字段
     *
     * @param name
     * @param value
     */
    public void addFormField(String name, Object value) {
        queryParams.put(name, value);
    }

    /**
     * 添加参数字段
     */
    public void addFormField(Map<String, Object> queryParams) {
        this.queryParams = queryParams;
    }

    /**
     * 添加请求头
     *
     * @param key
     * @param value
     */
    public void addHeader(String key, String value) {
        httpConn.setRequestProperty(key, value);
    }

    /**
     * 将请求字段转化成byte数组
     *
     * @param params
     * @return
     */
    private byte[] getParamsByte(Map<String, Object> params) {
        byte[] result = null;
        StringBuilder postData = new StringBuilder();
        for (Map.Entry<String, Object> param : params.entrySet()) {
            if (postData.length() != 0) {
                postData.append('&');
            }
            postData.append(this.encodeParam(param.getKey()));
            postData.append('=');
            postData.append(this.encodeParam(String.valueOf(param.getValue())));
        }
        try {
            result = postData.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 对键和值进行url编码
     *
     * @param data
     * @return
     */
    private String encodeParam(String data) {
        String result = "";
        try {
            result = URLEncoder.encode(data, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 完成请求，并接受服务器的回应
     *
     * @return 如果请求成功，状态码是200，返回服务器返回的字符串，否则抛出异常
     * @throws IOException
     */
    public String finish() throws IOException {
        String response = "";
        byte[] postDataBytes = this.getParamsByte(queryParams);
        httpConn.getOutputStream().write(postDataBytes);
        // 检查服务器返回状态
        int status = httpConn.getResponseCode();
        if (status == HttpURLConnection.HTTP_OK) {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = httpConn.getInputStream().read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            response = result.toString(this.charset);
            httpConn.disconnect();
        } else {
            throw new IOException("Server returned non-OK status: " + status);
        }
        return response;
    }
}

