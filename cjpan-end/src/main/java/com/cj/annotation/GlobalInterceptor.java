    package com.cj.annotation;

    import org.springframework.web.bind.annotation.Mapping;

    import java.lang.annotation.*;

    /**
     * @Classname GloballInterceptor
     * @Description 标注需要校验的方法~
     * @Date 2024/3/5 8:32
     * @Created by 憧憬
     */
    @Target({ElementType.METHOD}) // 标注使用注解的地方
    @Retention(RetentionPolicy.RUNTIME) // 运行时保留 可通过反射获取
    @Mapping // 表示可能为自定义注解
    @Documented // 表示该注解应该包含在java文档中
    public @interface GlobalInterceptor {
        /**
         * 校验参数
         */
        boolean checkParam() default false;

        /**
         * 校验登录
         */
        boolean checkLogin() default true;

        /**
         * 校验管理员
         */
        boolean checkAdmin() default false;



    }
