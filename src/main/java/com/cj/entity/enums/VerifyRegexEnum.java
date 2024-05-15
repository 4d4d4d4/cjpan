package com.cj.entity.enums;

import lombok.Getter;

/**
 * @Classname VerifyRegexEnum
 * @Description 什么也没有写哦~
 * @Date 2024/3/5 8:57
 * @Created by 憧憬
 */
@Getter
public enum VerifyRegexEnum {
    No("","不校验"),
    EMAIL("^[\\w-]+(\\.[\\w-]+)*@[\\w-]+(\\.[\\w-]+)+$", "邮箱"),
    PASSWORD("^(?=.*\\d)(?=.*[a-zA-Z])[\\da-zA-Z~!@#$%^&*_]{8,}$", "只能是数字，字母，特殊字符 8-18位");
    private String regex; // 正则表达式
    private String desc; // 注释

    VerifyRegexEnum() {
    }
    VerifyRegexEnum(String regex, String desc) {
        this.regex = regex;
        this.desc = desc;
    }
}
