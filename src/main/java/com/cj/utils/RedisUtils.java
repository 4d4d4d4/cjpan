package com.cj.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @Classname RedisUtils
 * @Description 什么也没有写哦~
 * @Date 2024/3/3 16:26
 * @Created by 憧憬
 */
@Component
public  class  RedisUtils<V> {
    @Resource
    private RedisTemplate<String,V> redisTemplate;
    private static final Logger logger = LoggerFactory.getLogger(RedisUtils.class);

    public V get(String key){
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }

    public boolean set(String key, V value){
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e){
            logger.error("设置redisKey:{},value:{}失败\n,原因：{}", key , value, e.getMessage());
            return false;
        }
    }

    public boolean setEx(String key, V value, long time){
        try {
            if(time > 0){
                redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
            }else {
                redisTemplate.opsForValue().set(key, value);
            }
            return true;
        }catch (Exception e){
            logger.error("设置redisKey:{},value:{}失败,原因：{}", key, value, e.getMessage());
            return false;
        }
    }

    public boolean remove(String s) {
        try {
            if(StringTools.isEmpty(s)){
                return false;
            }
           return redisTemplate.delete(s);
        }catch (Exception e){
            logger.error("邮箱删除失败，原因：{}",e.getMessage());
            return false;
        }
    }
}
