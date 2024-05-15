package com.cj.entity.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

/**
 * @Classname UploadResultDto
 * @Description 上传文件结果返回对象
 * @Date 2024/3/12 15:39
 * @Created by 憧憬
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UploadResultDto implements Serializable {
    private String fileId;  // 上传文件id
    private String status; // 上传文件的状态

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public UploadResultDto() {
    }


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UploadResultDto(String fileId, String status) {
        this.fileId = fileId;
        this.status = status;
    }
}
