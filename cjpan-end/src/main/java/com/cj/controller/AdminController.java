package com.cj.controller;

import com.cj.annotation.GlobalInterceptor;
import com.cj.annotation.VerifyParam;
import com.cj.component.RedisComponent;
import com.cj.entity.dto.SessionWebUserDto;
import com.cj.entity.dto.SysSettingsDto;
import com.cj.entity.query.FileInfoQuery;
import com.cj.entity.query.UserInfoQuery;
import com.cj.entity.vo.PaginationResultVO;
import com.cj.entity.vo.ResponseVO;
import com.cj.entity.vo.UserInfoVO;
import com.cj.service.FileInfoService;
import com.cj.service.UserInfoService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
// getUseSpace
@RestController("adminController")
@RequestMapping("/admin")
public class AdminController extends CommonFileController {

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private FileInfoService fileInfoService;

    @RequestMapping("/getSysSettings")
    @GlobalInterceptor(checkParam = true, checkAdmin = true)
    public ResponseVO getSysSettings() {
        return getSuccessResponseVO(redisComponent.getSysSettingsDto());
    }


    @RequestMapping("/saveSysSettings")
    @GlobalInterceptor(checkParam = true, checkAdmin = true)
    public ResponseVO saveSysSettings(
            @VerifyParam(require = true) String registerEmailTitle,
            @VerifyParam(require = true) String registerEmailContent,
            @VerifyParam(require = true) Integer userInitUseSpace) {
        SysSettingsDto sysSettingsDto = new SysSettingsDto();
        sysSettingsDto.setRegisterMailTitle(registerEmailTitle);
        sysSettingsDto.setRegisterEmailContent(registerEmailContent);
        sysSettingsDto.setUserInitUseSpace(userInitUseSpace);
        redisComponent.saveSysSettingsDto(sysSettingsDto);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/loadUserList")
    @GlobalInterceptor(checkParam = true, checkAdmin = true)
    public ResponseVO loadUser(UserInfoQuery userInfoQuery) {
        userInfoQuery.setOrderBy("join_time desc");
        PaginationResultVO resultVO = userInfoService.findListByPage(userInfoQuery);
        return getSuccessResponseVO(convert2PaginationVO(resultVO, UserInfoVO.class));
    }


    @RequestMapping("/updateUserStatus")
    @GlobalInterceptor(checkParam = true, checkAdmin = true)
    public ResponseVO updateUserStatus(@VerifyParam(require = true) String userId, @VerifyParam(require = true) Integer status) {
        userInfoService.updateUserStatus(userId, status);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/updateUserSpace")
    @GlobalInterceptor(checkParam = true, checkAdmin = true)
    public ResponseVO updateUserSpace(@VerifyParam(require = true) String userId, @VerifyParam(require = true) Long changeSpace) {
        userInfoService.changeUserSpace(userId, changeSpace);
        return getSuccessResponseVO(null);
    }

    /**
     * 查询所有文件
     *
     * @param query
     * @return
     */
    @RequestMapping("/loadFileList")
    @GlobalInterceptor(checkParam = true,checkAdmin = true)
    public ResponseVO loadDataList(FileInfoQuery query) {
        query.setOrderBy("last_update_time desc");
        query.setQueryNickName(true);
        PaginationResultVO resultVO = fileInfoService.findListByPage(query);
        return getSuccessResponseVO(resultVO);
    }

    @RequestMapping("/getFolderInfo")
    @GlobalInterceptor(checkLogin = false,checkAdmin = true, checkParam = true)
    public ResponseVO getFolderInfo(@VerifyParam(require = true) String path) {
        return super.getFolderInfo(null , path, null);
    }


    @RequestMapping("/getFile/{userId}/{fileId}")
    @GlobalInterceptor(checkParam = true, checkAdmin = true)
    public void getFiles(HttpServletResponse response,
                        @PathVariable("userId") @VerifyParam(require = true) String userId,
                        @PathVariable("fileId") @VerifyParam(require = true) String fileId) {
        SessionWebUserDto user = new SessionWebUserDto();
        user.setUserId(userId);
        super.getVideoInfo(response, user, fileId);
    }


    @RequestMapping("/ts/getVideoInfo/{userId}/{fileId}")
    @GlobalInterceptor(checkParam = true, checkAdmin = true)
    public void getVideoInfo(HttpServletResponse response,
                             @PathVariable("userId") @VerifyParam(require = true) String userId,
                             @PathVariable("fileId") @VerifyParam(require = true) String fileId) {
        SessionWebUserDto user = new SessionWebUserDto();
        user.setUserId(userId);
        super.getVideoInfo(response, user, fileId);
    }

    @RequestMapping("/createDownloadUrl/{userId}/{fileId}")
    @GlobalInterceptor(checkParam = true, checkAdmin = true)
    public ResponseVO createDownloadUrl(@PathVariable("userId") @VerifyParam(require = true) String userId,
                                        @PathVariable("fileId") @VerifyParam(require = true) String fileId) {
        return super.createDownloadUrl(fileId, userId);
    }

    /**
     * 下载
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/download/{code}")
    @GlobalInterceptor(checkLogin = false, checkParam = true)
    public void download(HttpServletRequest request, HttpServletResponse response,
                         @PathVariable("code") @VerifyParam(require = true) String code) throws Exception {
        super.download(request, response, code);
    }


    @RequestMapping("/delFile")
    @GlobalInterceptor(checkParam = true, checkAdmin = true)
    public ResponseVO delFile(@VerifyParam(require = true) String fileIdAndUserIds) {
        String[] fileIdAndUserIdArray = fileIdAndUserIds.split(",");
        for (String fileIdAndUserId : fileIdAndUserIdArray) {
            String[] itemArray = fileIdAndUserId.split("_");
            fileInfoService.delFileBatch(itemArray[0], itemArray[1], true);
        }
        return getSuccessResponseVO(null);
    }
}
