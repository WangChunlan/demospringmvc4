package com.mymvcframework.annotation;


import java.lang.annotation.*;
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CLController {
    String value() default "";
}
