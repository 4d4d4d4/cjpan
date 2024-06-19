package com.cj.mappers;

import com.cj.entity.po.FileInfo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文件信息 数据库操作接口
 */
public interface FileInfoMapper<T, P> extends BaseMapper<T, P> {

    /**
     * 根据FileIdAndUserId更新
     */
    Integer updateByFileIdAndUserId(@Param("bean") T t, @Param("fileId") String fileId, @Param("userId") String userId);


    /**
     * 根据FileIdAndUserId删除
     */
    Integer deleteByFileIdAndUserId(@Param("fileId") String fileId, @Param("userId") String userId);


    /**
     * 根据FileIdAndUserId获取对象
     */
    T selectByFileIdAndUserId(@Param("fileId") String fileId, @Param("userId") String userId);

    Long selectUserSpaceByUserId(@Param("userId") String userId);

    void updateFileStatusWithOldStatus(@Param("userId") String userId, @Param("fileId") String fileId, @Param("bean") T t, @Param("oldStatus") Integer oldStatus);

    void updateFileDelFlagWithOldDelFlag(@Param("userId") String userId, @Param("fileId") String fileId, @Param("bean") T delFileInfo, @Param("oldFlag") Integer flag);

    Integer selectAllRecycleCount(@Param("userId") String userId, @Param("flag") Integer flag);

    void updateFileDelFlagBatch(@Param("bean") FileInfo fileInfo,
                                @Param("userId") String userId,
                                @Param("filePidList") List<String> filePidList,
                                @Param("fileIdList") List<String> fileIdList,
                                @Param("oldDelFlag") Integer oldDelFlag);

    void delFileBatch(@Param("userId") String userId,
                      @Param("filePidList") List<String> filePidList,
                      @Param("fileIdList") List<String> fileIdList,
                      @Param("oldDelFlag") Integer oldDelFlag);

    Long selectUseSpace(@Param("userId") String userId);


    // 删除回收站文件
    void delReycleFileBatch(@Param("ids") List<String> deleteIds, @Param("oldStatus") Integer oldStatus);

    List<T> findAllDelFile(@Param("query") P fileInfoQuery);

    // 从数据中删除文件
    @Delete("delete from file_info where file_id = #{fileId} and del_flag = #{flag}")
    void deleteFileById(@Param("fileId") String fileId, @Param("flag") Integer flag);
}
