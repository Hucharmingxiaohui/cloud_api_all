package com.df.server.dto.robotDog;

import lombok.Data;

/**
 * 智能分析接口响应
 */
@Data
public class AnalyseResponseDTO {
    /**
     * 响应code
     * 200：成功;
     * 400：表示客户端请求有语法错误，不能被服务器所理解;
     * 500：服务端异常
     */
    int code;
}
