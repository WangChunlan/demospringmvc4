package com.mymvcframework.annotation;

import java.lang.annotation.*;
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CLRequestParam {
    String value() default "";

    String name() default "";

    boolean required() default true;

}

