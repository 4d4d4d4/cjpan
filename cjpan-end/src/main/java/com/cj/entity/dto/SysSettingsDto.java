package com.cj.entity.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;

/**
 * @Classname SysSettingsDto
 * @Description 什么也没有写哦~
 * @Date 2024/3/3 17:01
 * @Created by 憧憬
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true) // 在反序列化时候忽略位置属性
public class SysSettingsDto implements Serializable {
    private String registerMailTitle = "邮箱验证码";
    private String registerEmailContent = "<html><body><h1>憧憬网盘</h1><p>您的网盘验证码为：%s</p><img src='cid:%s'></body></html>";
    private String emailCode = "12345";
    private Integer userInitUseSpace = 5;

    public String getEmailCode() {
        return emailCode;
    }

    public void setEmailCode(String emailCode) {
        this.emailCode = emailCode;
    }

    public String getRegisterMailTitle() {
        return registerMailTitle;
    }

    public void setRegisterMailTitle(String registerMailTitle) {
        this.registerMailTitle = registerMailTitle;
    }

    public String getRegisterEmailContent() {
        return registerEmailContent;
    }

    public void setRegisterEmailContent(String registerEmailContent) {
        this.registerEmailContent = registerEmailContent;
    }

    public Integer getUserInitUseSpace() {
        return userInitUseSpace;
    }

    public void setUserInitUseSpace(Integer userInitUseSpace) {
        this.userInitUseSpace = userInitUseSpace;
    }
}
