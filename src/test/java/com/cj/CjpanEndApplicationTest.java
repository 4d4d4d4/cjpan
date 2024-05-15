package com.cj;

import com.cj.component.RedisComponent;
import com.cj.entity.dto.SysSettingsDto;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;
import java.io.File;
import java.util.Random;

/**
 * @Classname CjpanEndApplicationTest
 * @Description 什么也没有写哦~
 * @Date 2024/3/1 20:18
 * @Created by 憧憬
 */
@SpringBootTest
public class CjpanEndApplicationTest {
    @Resource
    public RedisTemplate redisTemplate;
    @Test
    public void aa(){
        SysSettingsDto s = new SysSettingsDto();
        System.out.println(s);
        redisTemplate.opsForValue().set("test",s);
        System.out.println(redisTemplate.opsForValue().get("test"));
    }
    @Test
    public void main(){
        ApplicationHome applicationHome = new ApplicationHome(this.getClass());
        String sp = File.separator;
        String s = applicationHome.getDir().getAbsolutePath() + sp + "src" + sp +"main" + sp  + "resources" + sp + "static" + sp + RandomUtils.nextInt(1,4) + ".png";
        System.out.println(s);
    }
}
