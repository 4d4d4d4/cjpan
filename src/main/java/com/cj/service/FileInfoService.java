package com.cj.service;

import com.cj.entity.dto.SessionWebUserDto;
import com.cj.entity.dto.UploadResultDto;
import com.cj.entity.po.FileInfo;
import com.cj.entity.query.FileInfoQuery;
import com.cj.entity.query.SimplePage;
import com.cj.entity.vo.PaginationResultVO;
import org.apache.ibatis.annotations.Param;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;


/**
 * 文件信息 业务接口
 */
public interface FileInfoService {

    /**
     * 根据条件查询列表
     */
    List<FileInfo> findListByParam(FileInfoQuery param);

    /**
     * 根据条件查询列表
     */
    Integer findCountByParam(FileInfoQuery param);

    /**
     * 分页查询
     */
    PaginationResultVO<FileInfo> findListByPage(FileInfoQuery param);

    /**
     * 新增
     */
    Integer add(FileInfo bean);

    /**
     * 批量新增
     */
    Integer addBatch(List<FileInfo> listBean);

    /**
     * 批量新增/修改
     */
    Integer addOrUpdateBatch(List<FileInfo> listBean);

    /**
     * 根据FileIdAndUserId查询对象
     */
    FileInfo getFileInfoByFileIdAndUserId(String fileId, String userId);


    /**
     * 根据FileIdAndUserId修改
     */
    Integer updateFileInfoByFileIdAndUserId(FileInfo bean, String fileId, String userId);


    /**
     * 根据FileIdAndUserId删除
     */
    Integer deleteFileInfoByFileIdAndUserId(String fileId, String userId);

    UploadResultDto uploadFile(SessionWebUserDto userinfoFromSession, String fileId, MultipartFile file, String fileName, String filePid, String fileMd5, Integer chunkIndex, Integer chunks);

    Long selectUserSpaceByUserId(String userId);

    FileInfo newFolder(String filePid, String userId, String folderName);

    void reNameByUserIdAndFileId(String userId, String fileId, String newFileName);

    void changeFileFolder(String fileIds, String filePid, String userId);

    void deleteFileInfoByFileIdsAndUserId(String fileIds, String userId);

    Integer selectAllRecycleCount(String userId);

    List<FileInfo> selectAllRecycleListWithPage(String userId, SimplePage simplePage);

    void thoroughDelFilesInfoByFileIdsAndUserId(String fileIds, String userId);

    void recoverFileBatch(String userId, String fileIds);

    void delFileBatch(String userId, String fileIds, Boolean adminOp);

}