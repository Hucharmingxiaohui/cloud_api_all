package com.df.framework.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DfeRequiresPermissions {

    String[] value();

    DfeLogical logical() default DfeLogical.AND;
}
