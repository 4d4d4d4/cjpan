package com.cj.utils;

import com.cj.entity.enums.VerifyRegexEnum;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Classname VerifyUtils
 * @Description 参数校验工具类 格式校验
 * @Date 2024/3/5 10:33
 * @Created by 憧憬
 */
public class VerifyUtils {
    public static boolean verify(String regex, String value){
        if(StringTools.isEmpty(value)){
            return false;
        }
        Pattern compile = Pattern.compile(regex);
        Matcher matcher = compile.matcher(value);
        if(matcher.find()){
            return true;
        }
        return false;
    }
    public static boolean verify(VerifyRegexEnum regex, String value) {
        return verify(regex.getRegex(), value);
    }

}
