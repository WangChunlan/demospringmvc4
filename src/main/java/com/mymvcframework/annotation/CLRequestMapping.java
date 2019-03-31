package com.mymvcframework.annotation;

import java.lang.annotation.*;
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CLRequestMapping {
    String name() default "";
    String value() default "";

//    String[] value() default {};

    String[] path() default {};


    String[] params() default {};

    String[] headers() default {};

    String[] consumes() default {};

    String[] produces() default {};
}
