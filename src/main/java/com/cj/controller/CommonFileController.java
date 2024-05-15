package com.cj.controller;

import com.cj.annotation.GlobalInterceptor;
import com.cj.annotation.VerifyParam;
import com.cj.component.RedisComponent;
import com.cj.entity.config.AppConfig;
import com.cj.entity.constants.Constants;
import com.cj.entity.dto.DownloadFileDto;
import com.cj.entity.dto.SessionWebUserDto;
import com.cj.entity.enums.FileDelFlagEnums;
import com.cj.entity.enums.FileFolderTypeEnums;
import com.cj.entity.enums.FileStatusEnums;
import com.cj.entity.enums.ResponseCodeEnum;
import com.cj.entity.po.FileInfo;
import com.cj.entity.query.FileInfoQuery;
import com.cj.entity.vo.ResponseVO;
import com.cj.exception.BusinessException;
import com.cj.mappers.FileInfoMapper;
import com.cj.service.FileInfoService;
import com.cj.utils.StringTools;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.net.URLEncoder;
import java.util.List;

/**
 * @Classname CommonFileController
 * @Description 什么也没有写哦~
 * @Date 2024/3/16 15:36
 * @Created by 憧憬
 */
public class CommonFileController extends ABaseController{

    @Resource
    private FileInfoService fileInfoService;
    @Resource
    private FileInfoMapper<FileInfo, FileInfoQuery> fileInfoMapper;

    @Resource
    private AppConfig appConfig;
    @Resource
    private RedisComponent redisComponent;

    /**
     * 预览照片
     * @param response
     * @param imageDate 照片日期
     * @param imageName 照片名
     */
    protected void getImage(HttpServletResponse response, String imageDate, String imageName){
        if(StringTools.isEmpty(imageDate) || StringTools.isEmpty(imageName) || !StringTools.pathIsOk(imageName)){
            return;
        }
        String imageSuffix = StringTools.getFileSuffix(imageName).replace(".", "");
        String filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + imageDate + "/" + imageName;
        String contentType = "image/" + imageSuffix;
        response.setContentType(contentType);
        response.setHeader("Cache-Control", "max-age=2592000");
        readFile(response, filePath);
    }

    /**
     * 预览视频
     * @param response 响应
     * @param userDto 用户id
     * @param fileId 文件id
     */
    protected void getVideoInfo(HttpServletResponse response, SessionWebUserDto userDto, String fileId) {
        if(null == userDto || null == fileId){
            return;
        }
        String userId = userDto.getUserId();
        String filePath = null;
        FileInfo fileInfo = null;
        if(fileId.endsWith(".ts")){
            String realFileId = fileId.substring(0, fileId.lastIndexOf("_"));
            fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(realFileId, userId);
            filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + fileInfo.getFilePath().split("\\.")[0] + "/" + fileId; // month/fileId
        }else {
            fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(fileId, userId);
            filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + fileInfo.getFilePath().split("\\.")[0] + "/" + Constants.M3U8_NAME; // month/fileId
        }
        readFile(response, filePath);

    }

    /**
     * 获取文件夹详情
     * @param userDto 用户的id
      * @param path 文件夹id
     * @param shareId
     * @return
     */
    public ResponseVO getFolderInfo(SessionWebUserDto userDto,
                                    @VerifyParam(require = true) String path,
                                    @VerifyParam(require = true) String shareId){
        String[] filePaths = path.split("/");
        String userId = null;
        if(null != userDto){
            userId =  userDto.getUserId();
        }
        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setUserId(userId);
        fileInfoQuery.setFileIdArray(filePaths);
        fileInfoQuery.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        String orderBy = "field(file_id,\"" + StringUtils.join(filePaths, "\",\"") + "\")";
        fileInfoQuery.setOrderBy(orderBy);
        List<FileInfo> fileInfoList = fileInfoMapper.selectList(fileInfoQuery);
        return getSuccessResponseVO(fileInfoList);
    }

    public void download(HttpServletResponse response, String userId, String fileId){
        FileInfo fileInfo = fileInfoMapper.selectByFileIdAndUserId(fileId, userId);
        if(!FileStatusEnums.USING.getStatus().equals(fileInfo.getStatus()) || !FileDelFlagEnums.USING.getFlag().equals(fileInfo.getDelFlag())) {
            return;
        }
        response.setContentType("multipart/form-data");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileInfo.getFileName());
        readFile(response, appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + fileInfo.getFilePath());
    }

    protected void download(HttpServletRequest request, HttpServletResponse response, String code) throws Exception {
        DownloadFileDto downloadFileDto = redisComponent.getDownloadCode(code);
        if (null == downloadFileDto) {
            return;
        }
        String filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + downloadFileDto.getFilePath();
        String fileName = downloadFileDto.getFileName();
        response.setContentType("application/x-msdownload; charset=UTF-8");
        if (request.getHeader("User-Agent").toLowerCase().indexOf("msie") > 0) {//IE浏览器
            fileName = URLEncoder.encode(fileName, "UTF-8");
        } else {
            fileName = new String(fileName.getBytes("UTF-8"), "ISO8859-1");
        }
        response.setHeader("Content-Disposition", "attachment;filename=\"" + fileName + "\"");
        readFile(response, filePath);
    }

    protected ResponseVO createDownloadUrl(String fileId, String userId) {
        FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(fileId, userId);
        if (fileInfo == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (FileFolderTypeEnums.FOLDER.getType().equals(fileInfo.getFolderType())) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        String code = StringTools.getRandomString(Constants.LENGTH_50);
        DownloadFileDto downloadFileDto = new DownloadFileDto();
        downloadFileDto.setDownloadCode(code);
        downloadFileDto.setFilePath(fileInfo.getFilePath());
        downloadFileDto.setFileName(fileInfo.getFileName());

        redisComponent.saveDownloadCode(code, downloadFileDto);

        return getSuccessResponseVO(code);
    }
}
