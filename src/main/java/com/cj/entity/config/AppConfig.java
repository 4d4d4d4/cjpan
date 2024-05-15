package com.cj.entity.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @Classname AppConfig
 * @Description 什么也没有写哦~
 * @Date 2024/3/1 19:56
 * @Created by 憧憬
 */
@Component
@Getter
public class AppConfig {
    @Value("${spring.mail.username}")
    private String sendEmailUsername;
    @Value("${admin.emails}")
    private String adminEmails;
    @Value("${project.folder}")
    private String projectFolder;
    @Value("${qq.app.id}")
    private String qqAppId;
    @Value("${qq.app.key}")
    private String qqAppKey;
    @Value("${qq.url.authorization}")
    private String qqUrlAuthorization;
    @Value("${qq.url.access.token}")
    private String qqUrlAccessToken;
    @Value("${qq.url.openid}")
    private String qqUrlOpenid;
    @Value("${qq.url.user.info}")
    private String qqUrlUserInfo;
    @Value("${qq.url.redirect}")
    private String qqUrlRedirect;
}
