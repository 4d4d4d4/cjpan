package com.cj.component;

import com.cj.entity.constants.Constants;
import com.cj.entity.dto.DownloadFileDto;
import com.cj.entity.dto.SysSettingsDto;
import com.cj.entity.dto.UserSpaceDto;
import com.cj.entity.po.FileInfo;
import com.cj.entity.po.UserInfo;
import com.cj.entity.query.FileInfoQuery;
import com.cj.entity.query.UserInfoQuery;
import com.cj.mappers.FileInfoMapper;
import com.cj.mappers.UserInfoMapper;
import com.cj.service.UserInfoService;
import com.cj.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @Classname RedisComponet
 * @Description 什么也没有写哦~
 * @Date 2024/3/3 17:00
 * @Created by 憧憬
 */
@Component
@Slf4j
public class RedisComponent {
    @Resource
    private RedisUtils redisUtils;

    @Resource
    private FileInfoMapper<FileInfo, FileInfoQuery> fileInfoMapper;
    @Autowired
    private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

    /**
     * 获取临时文件所占空间大小
     * @param userId 用户id
     * @param fileId 文件id
     */
    public Long getFileTempSize(String userId, String fileId) {
        Object tempSize = redisUtils.get(Constants.REDIS_KEY_USER_FILE_TEMP_SIZE + userId + fileId);
        if(null == tempSize) {
            return 0L;
        }
        if(tempSize instanceof Integer){
            return ((Integer)tempSize).longValue();
        }else if (tempSize instanceof Long){
            return (Long) tempSize;
        }else {
            return 0L;
        }
    }

    /**
     * 保存文件缓存
     * @param userId 用户id
     * @param fileId 文件id
     * @param fileSize 文件大小
     */
    public void saveFileTempSizeRedis(String userId, String fileId, Long fileSize){
        Long currentTempSize = getFileTempSize(userId, fileId);
        redisUtils.setEx(Constants.REDIS_KEY_USER_FILE_TEMP_SIZE + userId + fileId, currentTempSize + fileSize, Constants.REDIS_KEY_EXPIRES_ONE_HOUR);
    }

    /**
     * 获取系统设置
     *
     * @return
     */
    public SysSettingsDto getSysSettingsDto() {
        SysSettingsDto sysSettingsDto = (SysSettingsDto) redisUtils.get(Constants.REDIS_KEY_SYS_SETTING);
        if (sysSettingsDto == null) {
            sysSettingsDto = new SysSettingsDto();
            redisUtils.set(Constants.REDIS_KEY_SYS_SETTING, sysSettingsDto);
        }
        return sysSettingsDto;
    }
    public SysSettingsDto getSysSettingsDto(String toEmail){
        String key = Constants.REDIS_QQ_EMAIL_KEY + toEmail;
        log.error(key);
        SysSettingsDto dto = (SysSettingsDto) redisUtils.get(key);
        if(null == dto){
            dto = new SysSettingsDto();
        }
        return dto;
    }

    /**
     *
     * 保存用户的储存空间 2小时
     * @param userId
     * @param userSpaceDto
     */
    public void saveUserSpaceUse(String userId, UserSpaceDto userSpaceDto) {
        redisUtils.setEx(Constants.REDIS_KEY_USER_SPACE_USE + userId, userSpaceDto, Constants.REDIS_KEY_EXPIRES_ONE_MIN * 120);
    }

    /**
     * 获取用户使用的空间
     * getUseSpace
     * @param userId
     * @return
     */
    public UserSpaceDto getUserSpaceUse(String userId) {
        UserSpaceDto spaceDto = (UserSpaceDto) redisUtils.get(Constants.REDIS_KEY_USER_SPACE_USE + userId);
        if (null == spaceDto) {
            spaceDto = new UserSpaceDto();
            Long userspace = fileInfoMapper.selectUserSpaceByUserId(userId);

            spaceDto.setUseSpace(userspace);
            UserInfo userInfoByUserId = userInfoMapper.selectByUserId(userId);
            spaceDto.setTotalSpace(userInfoByUserId.getTotalSpace());
            saveUserSpaceUse(userId, spaceDto);
        }
        return spaceDto;
    }

    /**
     * 保存设置
     *
     * @param sysSettingsDto
     */
    public void saveSysSettingsDto(SysSettingsDto sysSettingsDto) {
        redisUtils.set(Constants.REDIS_KEY_SYS_SETTING, sysSettingsDto);
    }

    public void saveDownloadCode(String code, DownloadFileDto downloadFileDto) {
        redisUtils.setEx(Constants.REDIS_KEY_DOWNLOAD + code, downloadFileDto, Constants.REDIS_KEY_EXPIRES_FIVE_MIN);
    }
    public DownloadFileDto getDownloadCode(String code) {
        return (DownloadFileDto) redisUtils.get(Constants.REDIS_KEY_DOWNLOAD + code);
    }
}
