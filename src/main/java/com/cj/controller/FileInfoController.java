package com.cj.controller;

import com.cj.annotation.GlobalInterceptor;
import com.cj.annotation.VerifyParam;
import com.cj.entity.dto.SessionWebUserDto;
import com.cj.entity.dto.UploadResultDto;
import com.cj.entity.enums.FileCategoryEnums;
import com.cj.entity.enums.FileDelFlagEnums;
import com.cj.entity.enums.FileFolderTypeEnums;
import com.cj.entity.po.FileInfo;
import com.cj.entity.query.FileInfoQuery;
import com.cj.entity.vo.FileInfoVO;
import com.cj.entity.vo.PaginationResultVO;
import com.cj.entity.vo.ResponseVO;
import com.cj.service.FileInfoService;
import com.cj.utils.CopyUtils;
import com.cj.utils.StringTools;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * &#064;Description  操作文件
 * &#064;Date  2024/3/12 14:05
 * &#064;Created  by 憧憬
 */
@RestController
@RequestMapping("/file")
public class FileInfoController extends CommonFileController {
    @Resource
    private FileInfoService fileInfoService;

    /**
     * 根据条件分页查询
     */
    @RequestMapping("loadDataList")
    @GlobalInterceptor
    public ResponseVO loadDataList(HttpSession session, FileInfoQuery fileInfoQuery, String category){
        // 确定文件类型
        FileCategoryEnums code = FileCategoryEnums.getCode(category);
        // 获取当前用户信息
        SessionWebUserDto userinfoFromSession = getUserinfoFromSession(session);
        fileInfoQuery.setUserId(userinfoFromSession.getUserId());
        // 按照时间倒叙排列
        fileInfoQuery.setOrderBy("last_update_time desc");
        // 查询正常的文件
        fileInfoQuery.setDelFlag(FileDelFlagEnums.USING.getFlag());
        if(null != code){
            fileInfoQuery.setFileCategory(code.getCategory());
        }
        // 转化为将listByPage中的list类型由fileInfo转化为FileInfoVO 对象
        PaginationResultVO<FileInfo> listByPage = fileInfoService.findListByPage(fileInfoQuery);
        return getSuccessResponseVO(convert2PaginationVO(listByPage, FileInfoVO.class));
    }

    /**
     * 上传文件
     * @param session 当前用户的session
     * @param fileId 文件id
     * @param file （切片的）文件
     * @param fileName 文件名
     * @param filePid 文件的父级id
     * @param fileMd5 文件的md5值
     * @param chunkIndex 切片标记
     * @param chunks 总切片数
     * @return
     */
    @RequestMapping("uploadFile")
    @GlobalInterceptor(checkParam = true, checkLogin = true)
    public ResponseVO uploadFile(HttpSession session,
                                 String fileId,
                                 @VerifyParam(require = true) MultipartFile file,
                                 @VerifyParam(require = true) String fileName,
                                 @VerifyParam(require = true) String filePid,
                                 @VerifyParam(require = true) String fileMd5,
                                 @VerifyParam(require = true) Integer chunkIndex,
                                 @VerifyParam(require = true) Integer chunks
                                 ){
        SessionWebUserDto userinfoFromSession = getUserinfoFromSession(session);
        UploadResultDto uploadResultDto = fileInfoService.uploadFile(userinfoFromSession, fileId, file, fileName, filePid, fileMd5, chunkIndex, chunks);

        return getSuccessResponseVO(uploadResultDto);
    }

    /**
     * 获取图片
     * @param session 用于获取当前用户
     * @param response 响应回图片
     * @param imageDate 照片时间
     * @param imageName 照片名
     */
    @RequestMapping("/getImage/{imageDate}/{imageName}")
    @GlobalInterceptor(checkParam = true)
    public void getImage(HttpSession session,
                               HttpServletResponse response,
                               @PathVariable("imageDate") @VerifyParam(require = true) String imageDate,
                               @PathVariable("imageName") @VerifyParam(require = true) String imageName){
        super.getImage(response, imageDate, imageName);
    }

    @RequestMapping("/ts/getVideoInfo/{fileId}")
    @GlobalInterceptor(checkParam = true)
    public ResponseVO getVideoInfo(HttpSession session,
                                   HttpServletResponse response,
                                   @PathVariable("fileId") String fileId){
        SessionWebUserDto userDto = getUserinfoFromSession(session);
        super.getVideoInfo(response, userDto, fileId);
        return getSuccessResponseVO(null);
    }

    /**
     * 新建文件夹
     * @param session 用于获取当前用户
     * @param filePid 文件父级id
     * @param fileName 文件名
     * @return
     */

    @RequestMapping("/newFoloder")
    @GlobalInterceptor(checkParam = true)
    public ResponseVO newFolder(HttpSession session,
                                @VerifyParam(require = true) String filePid,
                                @VerifyParam(require = true) String fileName){
        SessionWebUserDto user = getUserinfoFromSession(session);
        FileInfo fileInfo = fileInfoService.newFolder(filePid, user.getUserId(), fileName);
        return getSuccessResponseVO(fileInfo);
    }

    /**
     * 查看文件夹详情
     * @param session 获取当前用户
     * @param path 文件父级id
     * @param shareId 文件名
     * @return
     */
    @RequestMapping("/getFolderInfo")
    @GlobalInterceptor(checkParam = true)
    public ResponseVO getFolderInfo(HttpSession session,
                                @VerifyParam(require = true) @RequestParam("path") String path,
                                @VerifyParam(require = false) @RequestParam("shareId") String shareId){
        SessionWebUserDto user = getUserinfoFromSession(session);
        return getFolderInfo(user, path, shareId);
    }

    /**
     * 文件重命名
     * @param fileId 要重命名文件id
     * @param newFileName 新的名字
     */
    @RequestMapping("/rename")
    @GlobalInterceptor(checkParam = true)
    public ResponseVO reName(HttpSession session,
                             @VerifyParam(require = true) String fileId,
                             @VerifyParam(require = true) @RequestParam("fileName") String newFileName){
        SessionWebUserDto userinfo = getUserinfoFromSession(session);
        fileInfoService.reNameByUserIdAndFileId(userinfo.getUserId(), fileId, newFileName);
        return getSuccessResponseVO(null);
    }

    /**
     * 查询所有父级目录的文件 但不包括当前的
     * @param filePid 要查询的父级id
     * @param currentFileIds 当前文件的id
     */
    @RequestMapping("/loadAllFolder")
    @GlobalInterceptor(checkParam = true)
    public ResponseVO loadAllFolder(HttpSession session,
                                    @VerifyParam(require = true) String filePid,
                                    @VerifyParam(require = false) String currentFileIds){
        FileInfoQuery query = new FileInfoQuery();
        query.setUserId(getUserinfoFromSession(session).getUserId());
        query.setFilePid(filePid);
        query.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        if (!StringTools.isEmpty(currentFileIds)) {
            query.setExcludeFileIdArray(currentFileIds.split(",")); // 如果是一个文件夹的话 就不可以显示
        }
        query.setDelFlag(FileDelFlagEnums.USING.getFlag());
        query.setOrderBy("create_time desc");
        List<FileInfo> fileInfoList = fileInfoService.findListByParam(query);
        return getSuccessResponseVO(CopyUtils.copyList(fileInfoList, FileInfoVO.class));
    }

    /**
     * 移动文件
     * @param fileIds 要移动的文件id
     * @param filePid 目的父id
     */
    @RequestMapping("/changeFileFolder")
    @GlobalInterceptor(checkParam = true)
    public ResponseVO changeFileFolder(HttpSession session,
                                       @VerifyParam(require = true) String fileIds,
                                       @VerifyParam(require = true) String filePid) {
        SessionWebUserDto webUserDto = getUserinfoFromSession(session);
        fileInfoService.changeFileFolder(fileIds, filePid, webUserDto.getUserId());
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/createDownloadUrl/{fileId}")
    @GlobalInterceptor(checkParam = true)
    public ResponseVO createDownLoadUrl(HttpSession session,
                                        HttpServletResponse response,
                                        @VerifyParam(require = true) @PathVariable("fileId") String fileId){
        SessionWebUserDto userDto = getUserinfoFromSession(session);
        FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(fileId, userDto.getUserId());
        String filePath = fileInfo.getFilePath();
        readFile(response, filePath);
        return getSuccessResponseVO(fileId);

    }

    /**
     * 下载文件
     * @param response
     * @param fileId 需要下载的文件的id
     */
    @RequestMapping("/download/{code}")
    @GlobalInterceptor(checkParam = true)
    public void download(HttpSession session, HttpServletResponse response, @VerifyParam(require = true) @PathVariable("code") String fileId){
        SessionWebUserDto userDto = getUserinfoFromSession(session);
        download(response, userDto.getUserId(), fileId);
    }

    /**
     * 删除文件
     * @param fileIds 删除文件的ids
     */
    @RequestMapping("delFile")
    @GlobalInterceptor(checkParam = true)
    public ResponseVO delFile(HttpSession session, @VerifyParam(require = true) String fileIds){
        SessionWebUserDto user = getUserinfoFromSession(session);
        fileInfoService.deleteFileInfoByFileIdsAndUserId(fileIds, user.getUserId());
        return getSuccessResponseVO(null);
    }


}

