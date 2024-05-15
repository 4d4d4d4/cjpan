package com.cj.utils;

import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Classname CopyUtils
 * @Description 将一个对象转化为另一个对象
 * @Date 2024/3/12 14:22
 * @Created by 憧憬
 */
public class CopyUtils<S,R> {
    public static <S,R> List<R> copyList(List<S> origin, Class<R> purpose){
        List<R> rList = new ArrayList<>();
        R r = null;
        for(S t : origin){
            try {
                r = purpose.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            BeanUtils.copyProperties(t, r);
            rList.add(r);
        }
        return rList;
    }
    public static <S,R> R copy(S origin, Class<R> purpose){
        R r = null;
        try {
             r = purpose.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        BeanUtils.copyProperties(origin, r);
        return r;
    }
}
