package com.cj.entity.dto;

import java.io.Serializable;

/**
 * @Classname UserSpaceDto
 * @Description 什么也没有写哦~
 * @Date 2024/3/6 0:35
 * @Created by 憧憬
 */
public class UserSpaceDto implements Serializable {
    private Long useSpace;
    private Long totalSpace;

    public UserSpaceDto() {
    }

    public UserSpaceDto(Long useSpace, Long totalSpace) {
        this.useSpace = useSpace;
        this.totalSpace = totalSpace;
    }

    public Long getUseSpace() {
        return useSpace;
    }

    public void setUseSpace(Long useSpace) {
        this.useSpace = useSpace;
    }

    public Long getTotalSpace() {
        return totalSpace;
    }

    public void setTotalSpace(Long totalSpace) {
        this.totalSpace = totalSpace;
    }
}
