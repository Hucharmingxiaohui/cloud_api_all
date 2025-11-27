package com.dji.sample.configuration;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 直接拦截 UnsupportedOperationException 的 AOP
 */
@Aspect
@Component
public class UnsupportedOperationAspect {

    private static final Logger log = LoggerFactory.getLogger(UnsupportedOperationAspect.class);

    /**
     * 拦截所有方法，捕获 UnsupportedOperationException
     */
    @Around("execution(* com.dji.sdk..*(..))") // 拦截所有大疆包下的方法
    public Object interceptUnsupportedOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            return joinPoint.proceed();
        } catch (UnsupportedOperationException e) {
            // 检查是否为 "not implemented" 异常
            if (e.getMessage() != null && e.getMessage().contains("not implemented")) {
                // 完全抑制这个异常，不记录 ERROR 日志
                if (log.isDebugEnabled()) {
                    log.debug("🔇 抑制未实现方法: {} -> {}",
                            joinPoint.getSignature().getName(), e.getMessage());
                }
                // 返回 null 或适当的默认值
                return null;
            }
            // 其他 UnsupportedOperationException 正常抛出
            throw e;
        }
    }
}
