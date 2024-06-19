package com.cj.service.impl;

import com.cj.component.RedisComponent;
import com.cj.entity.config.AppConfig;
import com.cj.entity.constants.Constants;
import com.cj.entity.dto.SessionWebUserDto;
import com.cj.entity.dto.UploadResultDto;
import com.cj.entity.dto.UserSpaceDto;
import com.cj.entity.enums.*;
import com.cj.entity.po.FileInfo;
import com.cj.entity.po.UserInfo;
import com.cj.entity.query.FileInfoQuery;
import com.cj.entity.query.SimplePage;
import com.cj.entity.query.UserInfoQuery;
import com.cj.entity.vo.PaginationResultVO;
import com.cj.exception.BusinessException;
import com.cj.mappers.FileInfoMapper;
import com.cj.mappers.UserInfoMapper;
import com.cj.service.FileInfoService;
import com.cj.service.UserInfoService;
import com.cj.utils.DateUtil;
import com.cj.utils.ProcessUtils;
import com.cj.utils.ScaleFilter;
import com.cj.utils.StringTools;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * 文件信息 业务接口实现
 */
@Service("fileInfoService")
public class FileInfoServiceImpl implements FileInfoService {

    private static final Logger logger = LoggerFactory.getLogger(FileInfoServiceImpl.class);

    @Resource
    @Lazy // 懒加载
    private FileInfoServiceImpl fileInfoService;

    @Resource
    private AppConfig appConfig;


    @Resource
    private FileInfoMapper<FileInfo, FileInfoQuery> fileInfoMapper;

    @Resource
    private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

    @Resource
    private RedisComponent redisComponent;


    /**
     * 根据条件查询列表
     */
    @Override
    public List<FileInfo> findListByParam(FileInfoQuery param) {
        return this.fileInfoMapper.selectList(param);
    }

    /**
     * 根据条件查询列表
     */
    @Override
    public Integer findCountByParam(FileInfoQuery param) {
        return this.fileInfoMapper.selectCount(param);
    }

    /**
     * 分页查询方法
     */
    @Override
    public PaginationResultVO<FileInfo> findListByPage(FileInfoQuery param) {
        int count = this.findCountByParam(param);
        int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

        // 当前页 数据总数 分页大小
        SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
        param.setSimplePage(page);
        List<FileInfo> list = this.findListByParam(param);
        // 符合条件的数据总数 当前页大小 当前页 总页数 分页条件查询的数据列表
        PaginationResultVO<FileInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
        return result;
    }

    /**
     * 新增
     */
    @Override
    public Integer add(FileInfo bean) {
        return this.fileInfoMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    @Override
    public Integer addBatch(List<FileInfo> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.fileInfoMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或者修改
     */
    @Override
    public Integer addOrUpdateBatch(List<FileInfo> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.fileInfoMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 根据FileIdAndUserId获取对象
     */
    @Override
    public FileInfo getFileInfoByFileIdAndUserId(String fileId, String userId) {
        return this.fileInfoMapper.selectByFileIdAndUserId(fileId, userId);
    }

    /**
     * 根据FileIdAndUserId修改
     */
    @Override
    public Integer updateFileInfoByFileIdAndUserId(FileInfo bean, String fileId, String userId) {
        return this.fileInfoMapper.updateByFileIdAndUserId(bean, fileId, userId);
    }

    /**
     * 根据FileIdAndUserId删除
     */
    @Override
    public Integer deleteFileInfoByFileIdAndUserId(String fileId, String userId) {
        return this.fileInfoMapper.deleteByFileIdAndUserId(fileId, userId);
    }

    /**
     * 文件上传
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UploadResultDto uploadFile(SessionWebUserDto user, String fileId,
                                      MultipartFile file, String fileName, String filePid,
                                      String fileMd5, Integer chunkIndex, Integer chunks) {
        Date now = new Date();
        Boolean uploadSuccess = true;
        File tempFileFolder = null;
        // 上传结果返回对象
        UploadResultDto resultDto = null;
        try {
            resultDto = new UploadResultDto();
            // 判断前端是否传入文件id 没有则生成
            if (StringUtils.isEmpty(fileId)) {
                fileId = UUID.randomUUID().toString().replace("-", "");
            }
            resultDto.setFileId(fileId);
            UserSpaceDto userSpaceUse = redisComponent.getUserSpaceUse(user.getUserId());

            // 判断是否是第一个文件切片
            if (chunkIndex == 0) {
                FileInfoQuery fileInfoQuery = new FileInfoQuery();
                fileInfoQuery.setUserId(user.getUserId());
                fileInfoQuery.setFileMd5(fileMd5);
                fileInfoQuery.setSimplePage(new SimplePage(0, 1));
                fileInfoQuery.setStatus(FileStatusEnums.USING.getStatus());
                fileInfoQuery.setDelFlag(FileDelFlagEnums.USING.getFlag());
                List<FileInfo> fileInfoList = fileInfoMapper.selectList(fileInfoQuery);
                redisComponent.saveFileTempSizeRedis(user.getUserId(), fileId, file.getSize());
                if (!fileInfoList.isEmpty()) {
                    // 秒传
                    FileInfo fileInfo = fileInfoList.get(0);
                    fileInfo.setCreateTime(now);
                    fileInfo.setFileMd5(fileMd5);
                    fileInfo.setLastUpdateTime(now);
                    fileInfo.setFilePid(filePid);
                    fileInfo.setFileId(fileId);
                    fileInfo.setLastUpdateTime(now);
                    fileInfo.setStatus(FileStatusEnums.USING.getStatus());
                    fileInfo.setFileName(reFileName(fileName, fileInfo.getFileMd5(), fileInfo.getUserId(), fileInfo.getFilePid(), null));
                    fileInfoMapper.insert(fileInfo);
                    // 修改用户空间
                    updateUserSpace(user.getUserId(), fileInfo.getFileSize());
                    resultDto.setStatus(UploadStatusEnums.UPLOAD_SECONDS.getCode());
                    return resultDto;
                }
            }

            // 判断磁盘空间
            Long currentTempSize = redisComponent.getFileTempSize(user.getUserId(), fileId);
            if (file.getSize() + currentTempSize + userSpaceUse.getUseSpace() > userSpaceUse.getTotalSpace()) {
                throw new BusinessException(ResponseCodeEnum.CODE_904);
            }

            // 暂存目录
            String tempFolderName = appConfig.getProjectFolder() + Constants.FILE_FOLDER_TEMP;
            String currentUserFolderName = user.getUserId() + fileId;
            tempFileFolder = new File(tempFolderName + currentUserFolderName + "/" + fileName);

            if (!tempFileFolder.exists()) {
                tempFileFolder.mkdirs();
            }
            // 存放切片
            File newFile = new File(tempFileFolder + "/" + chunkIndex);
            file.transferTo(newFile);
            if (chunkIndex < chunks - 1) {
                resultDto.setStatus(UploadStatusEnums.UPLOADING.getCode());
                // 保存切片大小
                redisComponent.saveFileTempSizeRedis(user.getUserId(), fileId, file.getSize());
                return resultDto;
            }

            // 最后一片 记录数据库 异步合并文件
            String month = DateUtil.format(new Date(), DateTimePatternEnum.YYYYMM.getPattern());
            // 真实文件名
            String fileSuffix = StringTools.getFileSuffix(fileName);
            String realFileName = currentUserFolderName + fileSuffix;
            // 自动重命名
            fileName = reFileName(fileName, fileMd5, user.getUserId(), filePid, null);

            FileInfo fileInfo = new FileInfo();
            fileInfo.setFileName(fileName);
            fileInfo.setUserId(user.getUserId());
            fileInfo.setFileMd5(fileMd5);
            fileInfo.setFilePath(month + "/" + realFileName);
            fileInfo.setFilePid(filePid);
            fileInfo.setCreateTime(now);
            fileInfo.setLastUpdateTime(now);
            fileInfo.setFileSize(chunkIndex * 1048576 + file.getSize()); // 计算文件大小 由之前文件切片数量 * 规定切片大小 + 当前切片大小
            fileInfo.setFileCategory(FileTypeEnums.getFileTypeBySuffix(fileSuffix).getCategory().getCategory()); // 文件范畴
            fileInfo.setFileType(FileTypeEnums.getFileTypeBySuffix(fileSuffix).getType()); // 文件具体类型
            fileInfo.setStatus(FileStatusEnums.TRANSFER.getStatus()); // 转码中文件 即合并
            fileInfo.setFolderType(FileFolderTypeEnums.FILE.getType());
            fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
            fileInfo.setFileId(fileId);
            this.fileInfoMapper.insert(fileInfo);

            // 更新用户磁盘
            Long totalSize = redisComponent.getFileTempSize(user.getUserId(), fileId);
            updateUserSpace(user.getUserId(), totalSize);
            resultDto.setStatus(UploadStatusEnums.UPLOAD_FINISH.getCode());
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    fileInfoService.transferFile(fileInfo.getFileId(), user);
                }
            });
            return resultDto;
        } catch (BusinessException e) {
            logger.error("文件上传失败");
            uploadSuccess = false;
            throw e;
        } catch (Exception e) {
            logger.error("文件上传失败");
            uploadSuccess = false;
            throw new BusinessException(e);
        } finally {
            // 删除临时文件夹
            if (!uploadSuccess && null != tempFileFolder) {
                try {
                    FileUtils.deleteDirectory(tempFileFolder);
                } catch (IOException e) {
                    logger.error("删除临时文件夹失败");
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * 修改用户空间
     *
     * @param userId   用户id
     * @param fileSize 修改大小
     */
    private void updateUserSpace(String userId, Long fileSize) {
        Integer count = userInfoMapper.updateUserSpace(userId, fileSize, null);
        if (count == 0) {
            throw new BusinessException(ResponseCodeEnum.CODE_904);
        }
        UserSpaceDto spaceDto = redisComponent.getUserSpaceUse(userId);
        spaceDto.setUseSpace(spaceDto.getUseSpace() + fileSize);
        redisComponent.saveUserSpaceUse(userId, spaceDto);
    }


    /**
     * 修改文件名
     *
     * @param fileName 原文件名
     * @param fileMd5  文件md5值
     * @param userId   用户id
     * @param filePid  用户所在文件夹
     * @param fileId   文件id
     * @return 返回的文件名  ffmpeg
     */
    private String reFileName(String fileName, String fileMd5, String userId, String filePid, String fileId) {
        FileInfoQuery md5CountQuery = new FileInfoQuery();
        md5CountQuery.setFileMd5(fileMd5);
        md5CountQuery.setUserId(userId);
        md5CountQuery.setFilePid(filePid);
        md5CountQuery.setStatus(FileStatusEnums.USING.getStatus());
        if (null != fileId && fileId != "") {
            md5CountQuery.setExcludeFileIdArray(fileId.split(","));
        }
        Integer md5Count = fileInfoMapper.selectCount(md5CountQuery);

        FileInfoQuery fileNameQuery = new FileInfoQuery();
        fileNameQuery.setFileName(fileName);
        fileNameQuery.setUserId(userId);
        fileNameQuery.setFilePid(filePid);
        fileNameQuery.setStatus(FileStatusEnums.USING.getStatus());
        if (null != fileId && fileId != "") {
            fileNameQuery.setExcludeFileIdArray(fileId.split(","));
        }
        Integer nameCount = fileInfoMapper.selectCount(fileNameQuery);

        Boolean isMd5 = md5Count > 0;
        Boolean isName = nameCount > 0;
        String realFileName = StringTools.getFileName(fileName);
        String fileSuffix = StringTools.getFileSuffix(fileName);
        // 文件名相同 md5值相同 --->
        if (isMd5 && isName) {
            return realFileName + "_副本(" + md5Count + ")" + fileSuffix;
        } else if (!isMd5 && isName) {
            // 文件名相同 md5不同
            throw new BusinessException("已存在相同文件名，请重命名");
//            return realFileName + "_(" + nameCount + ")" + fileSuffix;
        } else {
            // 文件名不同
            return fileName;
        }
    }

    /*
      根据用户id查询用户使用空间
    */
    @Override
    public Long selectUserSpaceByUserId(String userId) {
        Long userSpace = fileInfoMapper.selectUserSpaceByUserId(userId);
        return userSpace;
    }

    /*

     */
    @Override
    public FileInfo newFolder(String filePid, String userId, String folderName) {
        checkFileName(filePid, userId, folderName, FileFolderTypeEnums.FOLDER.getType());
        Date curDate = new Date();
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileId(StringTools.getRandomString(Constants.LENGTH_10));
        fileInfo.setUserId(userId);
        fileInfo.setFilePid(filePid);
        fileInfo.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        fileInfo.setCreateTime(curDate);
        fileInfo.setFileName(folderName);
        fileInfo.setLastUpdateTime(curDate);
        fileInfo.setStatus(FileStatusEnums.USING.getStatus());
        fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
        this.fileInfoMapper.insert(fileInfo);
        return fileInfo;
    }

    @Override
    public FileInfo reNameByUserIdAndFileId(String userId, String fileId, String newFileName) {
        FileInfo fileInfo = fileInfoMapper.selectByFileIdAndUserId(fileId, userId);
        if (!fileInfo.getStatus().equals(FileStatusEnums.USING.getStatus()) || !fileInfo.getDelFlag().equals(FileDelFlagEnums.USING.getFlag())) {
            throw new BusinessException("文件状态异常...");
        }
        // 完整重命名文件
        String fileSuffix = StringTools.getFileSuffix(fileInfo.getFileName());
        newFileName = newFileName + fileSuffix;
        // 查询重命名文件当前目录是否有于重命名文件重名文件
        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setFilePid(fileInfo.getFilePid());
        fileInfoQuery.setUserId(userId);
        fileInfoQuery.setFileName(newFileName);
        Integer count = fileInfoMapper.selectCount(fileInfoQuery);
        if (count > 0) {
            newFileName = reFileName(newFileName, fileInfo.getFileMd5(), userId, fileInfo.getFilePid(), fileInfo.getFileId());
        }
        fileInfo.setFileName(newFileName);
        fileInfo.setUserId(userId);
        fileInfo.setLastUpdateTime(new Date());
        fileInfoMapper.updateByFileIdAndUserId(fileInfo, fileId, userId);
        fileInfo = fileInfoMapper.selectByFileIdAndUserId(fileId, userId);

        return fileInfo;
    }

    @Override
    public void changeFileFolder(String fileIds, String filePid, String userId) {
        if (fileIds.equals(filePid)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (!Constants.ZERO_STR.equals(filePid)) {
            FileInfo fileInfo = fileInfoMapper.selectByFileIdAndUserId(filePid, userId);
            if (null == fileInfo || !FileStatusEnums.USING.getStatus().equals(fileInfo.getStatus())) {
                throw new BusinessException(ResponseCodeEnum.CODE_600);
            }
        }
        String[] ids = fileIds.split(",");
        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setUserId(userId);
        fileInfoQuery.setFilePid(filePid);
        // 查询移动至目的目录中的文件
        List<FileInfo> fileInfoList = fileInfoMapper.selectList(fileInfoQuery);
        Map<String, FileInfo> collect = fileInfoList.stream().collect(Collectors.toMap(FileInfo::getFileName, fileInfo -> {
            return fileInfo;
        }));
        // 查询要移动的文件
        fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setFileIdArray(ids);
        fileInfoQuery.setUserId(userId);
        fileInfoQuery.setStatus(FileStatusEnums.USING.getStatus());
        List<FileInfo> files = fileInfoMapper.selectList(fileInfoQuery);
        for (FileInfo f : files) {
            FileInfo fileInfo = collect.get(f.getFileName());
            if (null != fileInfo) {
                f.setFileName(reFileName(f.getFileName(), f.getFileMd5(), userId, filePid, null));
            }
            f.setFilePid(filePid);
            f.setLastUpdateTime(new Date());
            fileInfoMapper.updateByFileIdAndUserId(f, f.getFileId(), f.getUserId());
        }
    }

    @Override
    public void deleteFileInfoByFileIdsAndUserId(String fileIds, String userId) {
        String[] idList = fileIds.split(",");
        FileInfoQuery delFileListQuery = new FileInfoQuery();
        delFileListQuery.setUserId(userId);
        delFileListQuery.setFileIdArray(idList);
        delFileListQuery.setDelFlag(FileDelFlagEnums.USING.getFlag());
        delFileListQuery.setOrderBy(" recovery_time desc");
        List<FileInfo> delFileList = fileInfoMapper.selectList(delFileListQuery);

        delFile(delFileList, userId);
    }

    @Override
    public Integer selectAllRecycleCount(String userId) {
        return fileInfoMapper.selectAllRecycleCount(userId, FileDelFlagEnums.RECYCLE.getFlag());
    }

    @Override
    public List<FileInfo> selectAllRecycleListWithPage(String userId, SimplePage simplePage) {
        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setUserId(userId);
        fileInfoQuery.setSimplePage(simplePage);
        fileInfoQuery.setDelFlag(FileDelFlagEnums.RECYCLE.getFlag());
        return fileInfoMapper.selectList(fileInfoQuery);
    }

    @Override
    public void thoroughDelFilesInfoByFileIdsAndUserId(String fileIds, String userId) {
        String[] ids = fileIds.split(",");
        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setUserId(userId);
        fileInfoQuery.setDelFlag(FileDelFlagEnums.RECYCLE.getFlag());
        fileInfoQuery.setFileIdArray(ids);
        List<FileInfo> fileInfoList = fileInfoMapper.selectList(fileInfoQuery);

        for (FileInfo f : fileInfoList) {
            f.setDelFlag(FileDelFlagEnums.DEL.getFlag());
            f.setLastUpdateTime(new Date());
//            if (FileFolderTypeEnums.FILE.getType().equals(f.getFolderType())) {
//                boolean flag = FileUtils.deleteQuietly(new File(appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + f.getFilePath()));
//                if (!flag) {
//                    logger.error("删除文件{}，失败", f.getFileName());
//                }
//            }
            fileInfoMapper.updateByFileIdAndUserId(f, f.getFileId(), userId);

        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recoverFileBatch(String userId, String fileIds) {
        String[] fileIdArray = fileIds.split(",");
        FileInfoQuery query = new FileInfoQuery();
        query.setUserId(userId);
        query.setFileIdArray(fileIdArray);
        query.setDelFlag(FileDelFlagEnums.RECYCLE.getFlag());
        List<FileInfo> fileInfoList = this.fileInfoMapper.selectList(query);

        List<String> delFileSubFolderFileIdList = new ArrayList<>();
        //找到所选文件子目录文件ID
        for (FileInfo fileInfo : fileInfoList) {
            if (FileFolderTypeEnums.FOLDER.getType().equals(fileInfo.getFolderType())) {
                findAllSubFolderFileIdList(delFileSubFolderFileIdList, userId, fileInfo.getFileId(), FileDelFlagEnums.DEL.getFlag());
            }
        }
        //查询所有跟目录的文件
        query = new FileInfoQuery();
        query.setUserId(userId);
        query.setDelFlag(FileDelFlagEnums.USING.getFlag());
        query.setFilePid(Constants.ZERO_STR);
        List<FileInfo> allRootFileList = this.fileInfoMapper.selectList(query);

        Map<String, FileInfo> rootFileMap = allRootFileList.stream().collect(Collectors.toMap(FileInfo::getFileName, Function.identity(), (file1, file2) -> file2));

        //查询所有所选文件
        //将目录下的所有删除的文件更新为正常
        if (!delFileSubFolderFileIdList.isEmpty()) {
            FileInfo fileInfo = new FileInfo();
            fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
            this.fileInfoMapper.updateFileDelFlagBatch(fileInfo, userId, delFileSubFolderFileIdList, null, FileDelFlagEnums.DEL.getFlag());
        }
        //将选中的文件更新为正常,且父级目录到跟目录
        List<String> delFileIdList = Arrays.asList(fileIdArray);
        FileInfo fileInfo = new FileInfo();
        fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
        fileInfo.setFilePid(Constants.ZERO_STR);
        fileInfo.setRecoveryTime(null);
        fileInfo.setLastUpdateTime(new Date());
        this.fileInfoMapper.updateFileDelFlagBatch(fileInfo, userId, null, delFileIdList, FileDelFlagEnums.RECYCLE.getFlag());

        //将所选文件重命名
        for (FileInfo item : fileInfoList) {
            FileInfo rootFileInfo = rootFileMap.get(item.getFileName());
            //文件名已经存在，重命名被还原的文件名
            if (rootFileInfo != null) {
                String fileName = reFileName(item.getFileName(), item.getFileMd5(), userId, item.getFilePid(), item.getFileId());
                FileInfo updateInfo = new FileInfo();
                updateInfo.setFileName(fileName);
                this.fileInfoMapper.updateByFileIdAndUserId(updateInfo, item.getFileId(), userId);
            }
        }
    }

    private void delFile(List<FileInfo> delFileList, String userId) {
        for (FileInfo fileInfo : delFileList) {
            if (fileInfo.getFolderType().equals(FileFolderTypeEnums.FOLDER.getType())) {
                // 如果是目录
                String fileId = fileInfo.getFileId();
                FileInfoQuery delFileInfoQuery = new FileInfoQuery();
                // 查询其子目录
                delFileInfoQuery.setUserId(userId);
                delFileInfoQuery.setFilePid(fileId);
                delFileInfoQuery.setDelFlag(FileDelFlagEnums.USING.getFlag());
                List<FileInfo> delFileInfoList = fileInfoMapper.selectList(delFileInfoQuery);
                delFile(delFileInfoList, userId);
                // 将文件夹丢尽垃圾桶
                FileInfo f = new FileInfo();
                f.setUserId(userId);
                f.setFileId(fileId);
                f.setDelFlag(FileDelFlagEnums.RECYCLE.getFlag());
                f.setRecoveryTime(new Date());
                fileInfoMapper.updateFileDelFlagWithOldDelFlag(userId, fileId, f, FileDelFlagEnums.USING.getFlag());
            } else {
                // 如果是文件
                String fileId = fileInfo.getFileId();
                FileInfo delFileInfo = new FileInfo();
                // 将文件丢入垃圾桶
                delFileInfo.setUserId(userId);
                delFileInfo.setFileId(fileId);
                delFileInfo.setDelFlag(FileDelFlagEnums.RECYCLE.getFlag());
                delFileInfo.setLastUpdateTime(new Date());
                delFileInfo.setRecoveryTime(new Date()); // 设置加入回收站时间
                fileInfoMapper.updateFileDelFlagWithOldDelFlag(userId, fileId, delFileInfo, FileDelFlagEnums.USING.getFlag());
                // 更新用户空间
                userInfoMapper.updateUserSpace(userId, -fileInfo.getFileSize(), null);
                UserSpaceDto userSpaceUse = redisComponent.getUserSpaceUse(userId);
                userSpaceUse.setUseSpace(userSpaceUse.getUseSpace() - fileInfo.getFileSize());
                redisComponent.saveUserSpaceUse(userId, userSpaceUse);
            }
        }
    }

    // 根据文件夹id查询所有子文件
    private void findAllSubFolderFileIdList(List<String> fileIdList, String userId, String fileId, Integer delFlag) {
        fileIdList.add(fileId);
        FileInfoQuery query = new FileInfoQuery();
        query.setUserId(userId);
        query.setFilePid(fileId);
        query.setDelFlag(delFlag);
        query.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        List<FileInfo> fileInfoList = this.fileInfoMapper.selectList(query);
        for (FileInfo fileInfo : fileInfoList) {
            findAllSubFolderFileIdList(fileIdList, userId, fileInfo.getFileId(), delFlag);
        }
    }

    private void checkFileName(String filePid, String userId, String fileName, Integer folderType) {
        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setFolderType(folderType);
        fileInfoQuery.setFileName(fileName);
        fileInfoQuery.setFilePid(filePid);
        fileInfoQuery.setUserId(userId);
        fileInfoQuery.setDelFlag(FileDelFlagEnums.USING.getFlag());
        Integer count = this.fileInfoMapper.selectCount(fileInfoQuery);
        if (count > 0) {
            throw new BusinessException("此目录下已经存在同名文件，请修改名称");
        }
    }

    @Async
    public void transferFile(String fileId, SessionWebUserDto userDto) {
        Boolean transferSuccess = true; // 合并成功
        String targetFilePath = null; // 目标文件路径
        String cover = null; //
        FileTypeEnums fileTypeEnums = null; // 文件类型
        FileInfo fileInfo = this.fileInfoMapper.selectByFileIdAndUserId(fileId, userDto.getUserId());
        try {
            if (fileInfo == null || !FileStatusEnums.TRANSFER.getStatus().equals(fileInfo.getStatus())) {
                return;
            }
            // 临时目录
            String tempFolderName = appConfig.getProjectFolder() + Constants.FILE_FOLDER_TEMP;
            String currentUserFolderName = userDto.getUserId() + fileId;
            File fileFolder = new File(tempFolderName + currentUserFolderName);

            String fileSuffix = StringTools.getFileSuffix(fileInfo.getFileName());
            String month = DateUtil.format(fileInfo.getCreateTime(), DateTimePatternEnum.YYYYMM.getPattern());

            // 目标目录
            String targetFolderName = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE;
            File targetFolder = new File(targetFolderName + "/" + month);
            if (!targetFolder.exists()) {
                targetFolder.mkdirs();
            }
            // 真实文件名
            String realFileName = currentUserFolderName + fileSuffix;
            targetFilePath = targetFolder.getPath() + "/" + realFileName;

            // 合并文件
            union(fileFolder.getPath(), targetFilePath, fileInfo.getFileName(), false);

            System.out.println(fileInfo.getFileName());

            // 视频文件切割
            FileTypeEnums fileTypeBySuffix = FileTypeEnums.getFileTypeBySuffix(fileSuffix);
            if (fileTypeBySuffix == FileTypeEnums.VIDEO) {
                cutFile4Video(fileId, targetFilePath);
                // 视频生成缩略图
                cover = month + "/" + currentUserFolderName + Constants.IMAGE_PNG_SUFFIX;
                String coverPath = targetFolderName + "/" + cover;
                ScaleFilter.createCover4Video(new File(targetFilePath), Constants.LENGTH_150, new File(coverPath));
            } else if (FileTypeEnums.IMAGE == fileTypeBySuffix) {
                // 图片生成缩略图
                cover = month + "/" + realFileName.replace(".", "_.");
                String coverPath = targetFolderName + cover;
                Boolean thumbnailWidthFFmpeg = ScaleFilter.createThumbnailWidthFFmpeg(new File(targetFilePath), Constants.LENGTH_150, new File(coverPath), false);
                if (!thumbnailWidthFFmpeg) {
                    FileUtils.copyFile(new File(targetFilePath), new File(coverPath));
                }
            }
        } catch (Exception e) {
            logger.error("文件转码失败,文件ID:{},userId:{}", fileId, userDto.getUserId());
            transferSuccess = false;
            throw new BusinessException(e);
        } finally {
            FileInfo updateFile = new FileInfo();
            updateFile.setFileSize(new File(targetFilePath).length());
            updateFile.setFileCover(cover);
            updateFile.setStatus(transferSuccess ? FileStatusEnums.USING.getStatus() : FileStatusEnums.TRANSFER_FAIL.getStatus());
            fileInfoMapper.updateFileStatusWithOldStatus(userDto.getUserId(), fileId, updateFile, FileStatusEnums.TRANSFER.getStatus());
        }
    }

    /**
     * 合并文件
     *
     * @param dirPath    临时文件目录
     * @param toFilePath 整合完存放文件地址
     * @param fileName   文件名
     * @param delSource  是否删除临时文件目录
     */
    private void union(String dirPath, String toFilePath, String fileName, Boolean delSource) {
        File dir = new File(dirPath + "//" + fileName);
        if (!dir.exists()) {
            throw new BusinessException("目标目录不存在");
        }
        File[] fileList = dir.listFiles(); // 获取所有切片文件
//        Arrays.sort(fileList, Comparator.comparingInt(o -> Integer.parseInt(o.getName())));
        File targetFile = new File(toFilePath);
        // RandomAccessFile 文件进行随机访问的类 可读 也可以写在文件任意位置
        RandomAccessFile writeFile = null;
        try {
            writeFile = new RandomAccessFile(targetFile, "rw");
            byte[] bytes = new byte[1024 * 10];
            for (int i = 0; i < fileList.length; i++) {
                int len = -1;
                File file = new File(dirPath + "/" + fileName + "/" + i);
                logger.info("正在合并文件:{} ", file.getName()); // 日志统计，防止因合并顺序导致的错误
                RandomAccessFile readFile = null;
                try {
                    readFile = new RandomAccessFile(file, "r");
                    while ((len = readFile.read(bytes)) != -1) {
                        writeFile.write(bytes, 0, len);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.error("合并文件失败... {}", e.getMessage());
                    throw new BusinessException("合并文件失败");
                } finally {
                    readFile.close();
                }
            }
        } catch (Exception e) {
            logger.error("合并文件失败.... :{}", e.getMessage());
            throw new BusinessException("合并文件失败");
        } finally {
            if (null != writeFile) {
                try {
                    writeFile.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                if (delSource && dir.exists()) {
                    try {
                        FileUtils.deleteDirectory(dir);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    /**
     * 切割视频文件
     *
     * @param fileId        文件id
     * @param videoFilePath 视频文件路径
     */
    private void cutFile4Video(String fileId, String videoFilePath) {
        // 创建同名切片目录
        File tsFolder = new File(videoFilePath.substring(0, videoFilePath.lastIndexOf(".")));
        if (!tsFolder.exists()) {
            tsFolder.mkdirs();
        }
        final String CMD_TRANSFER_2TS = "ffmpeg -y -i %s -vcodec copy -acodec copy -bsf:v h264_mp4toannexb %s";
        final String CMD_CUT_TS = "ffmpeg -i %s -c copy -map 0 -f segment -segment_list %s -segment_time 30 %s/%s_%%4d.ts";
        String tsPath = tsFolder.getPath() + "/" + Constants.TS_NAME;
        // 生成ts文件
        String cmd = String.format(CMD_TRANSFER_2TS, videoFilePath, tsPath);
        ProcessUtils.executeCommand(cmd, false);
        System.out.println(cmd + "  *********************");
        // 生成索引文件.m3u8和.ts文件
        //ffmpeg -i [tsPath] -c copy -map 0 -f
        // segment -segment_list [tsFolder]/[M3U8_NAME]
        // -segment_time 30 [tsFolder]/[fileId]_%4d.ts [newParam]
        cmd = String.format(CMD_CUT_TS, tsPath, tsFolder.getPath() + "/" + Constants.M3U8_NAME, tsFolder.getPath(), fileId);
        ProcessUtils.executeCommand(cmd, false);
        System.out.println(cmd + "  *********************");
        // 删除index.ts文件
        File tsFile = new File(tsPath);
        tsFile.delete();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delFileBatch(String userId, String fileIds, Boolean adminOp) {
        String[] fileIdArray = fileIds.split(",");

        FileInfoQuery query = new FileInfoQuery();
        query.setUserId(userId);
        query.setFileIdArray(fileIdArray);
        if (!adminOp) {
            query.setDelFlag(FileDelFlagEnums.RECYCLE.getFlag());
        }
        List<FileInfo> fileInfoList = this.fileInfoMapper.selectList(query);
        List<String> delFileSubFolderFileIdList = new ArrayList<>();
        //找到所选文件子目录文件ID
        for (FileInfo fileInfo : fileInfoList) {
            if (FileFolderTypeEnums.FOLDER.getType().equals(fileInfo.getFolderType())) {
                findAllSubFolderFileIdList(delFileSubFolderFileIdList, userId, fileInfo.getFileId(), FileDelFlagEnums.DEL.getFlag());
            }
        }

        //删除所选文件，子目录中的文件
        if (!delFileSubFolderFileIdList.isEmpty()) {
            this.fileInfoMapper.delFileBatch(userId, delFileSubFolderFileIdList, null, adminOp ? null : FileDelFlagEnums.DEL.getFlag());
        }
        //删除所选文件
        this.fileInfoMapper.delFileBatch(userId, null, Arrays.asList(fileIdArray), adminOp ? null : FileDelFlagEnums.RECYCLE.getFlag());

        Long useSpace = this.fileInfoMapper.selectUseSpace(userId);
        UserInfo userInfo = new UserInfo();
        userInfo.setUseSpace(useSpace);
        this.userInfoMapper.updateByUserId(userInfo, userId);

        //设置缓存
        UserSpaceDto userSpaceDto = redisComponent.getUserSpaceUse(userId);
        userSpaceDto.setUseSpace(useSpace);
        redisComponent.saveUserSpaceUse(userId, userSpaceDto);

    }

    @Override
    public List<FileInfo> findAllByUserId(String userId) {
        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setUserId(userId);
        return fileInfoMapper.selectList(fileInfoQuery);
    }

    @Override
    public List<FileInfo> getFileInfoByUserIdAndPid(String userId, String filePid) {
        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setUserId(userId);
        fileInfoQuery.setFilePid(filePid);
        return fileInfoMapper.selectList(fileInfoQuery);
    }

    /**
     * 查询所有在回收站的文件
     * @param query
     * @return
     */
    @Override
    public List<FileInfo> findAllRecycleFile(FileInfoQuery query) {
        return fileInfoMapper.selectList(query);
    }

    /**
     * 删除所有文件
     * @param deleteIds
     */
    @Override
    public void deleteFileInfoByFileIds(List<String> deleteIds) {
        fileInfoMapper.delReycleFileBatch(deleteIds, FileDelFlagEnums.RECYCLE.getFlag());
    }

    /**
     * 查询所有被彻底删除的文件
     * @param fileInfoQuery
     * @return
     */
    @Override
    public List<FileInfo> findAllDelFile(FileInfoQuery fileInfoQuery) {
        return fileInfoMapper.findAllDelFile(fileInfoQuery);
    }

    @Override
    public void deleteFileByFileId(String fileId, Integer flag) {
        fileInfoMapper.deleteFileById(fileId, flag);
    }


}