package com.cj.annotation;

import com.cj.entity.enums.VerifyRegexEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Classname VerifyParam
 * @Description 标注需要校验的参数 及其校验的规则~
 * @Date 2024/3/5 8:42
 * @Created by 憧憬
 */
@Target({ElementType.PARAMETER,ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface VerifyParam {
    int min() default -1;
    int max() default -1;
    boolean require() default false;
    VerifyRegexEnum regex() default VerifyRegexEnum.No;
}
