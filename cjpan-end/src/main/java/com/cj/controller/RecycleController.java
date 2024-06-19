package com.cj.controller;

import com.cj.annotation.GlobalInterceptor;
import com.cj.annotation.VerifyParam;
import com.cj.component.RedisComponent;
import com.cj.entity.config.AppConfig;
import com.cj.entity.dto.SessionWebUserDto;
import com.cj.entity.po.FileInfo;
import com.cj.entity.query.SimplePage;
import com.cj.entity.vo.FileInfoVO;
import com.cj.entity.vo.ResponseVO;
import com.cj.mappers.FileInfoMapper;
import com.cj.service.FileInfoService;
import com.cj.utils.CopyUtils;
import com.cj.utils.JsonUtils;
import com.cj.utils.StringTools;
import com.cj.utils.VerifyUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * &#064;Classname  RecycleController
 * &#064;Description  垃圾桶控制
 * &#064;Date  2024/3/17 19:49
 * &#064;Created  by 憧憬
 */
@RequestMapping("/recycle")
@RestController
public class RecycleController extends CommonFileController {
    @Resource
    private FileInfoService fileInfoService;

    @Resource
    private AppConfig appConfig;
    @Resource
    private RedisComponent redisComponent;

    /**
     * 查询垃圾箱
     *
     * @param pageNo   当前页
     * @param pageSize 分页大小
     */
    @RequestMapping("/loadRecycleList")
    @GlobalInterceptor(checkParam = true)
    public ResponseVO loadRecycleList(HttpSession session,
                                      @VerifyParam(require = false) Integer pageNo,
                                      @VerifyParam(require = false) Integer pageSize) {
        SessionWebUserDto userDto = getUserinfoFromSession(session);
        if (pageSize == null) {
            pageSize = 15;
        }
        Integer count = fileInfoService.selectAllRecycleCount(userDto.getUserId());
        SimplePage simplePage = new SimplePage(pageNo, count, pageSize);
        List<FileInfo> list = fileInfoService.selectAllRecycleListWithPage(userDto.getUserId(), simplePage);
        List<FileInfoVO> fileInfoVOS = CopyUtils.copyList(list, FileInfoVO.class);
        Map<String, Object> result = new HashMap<>();
        result.put("totalCount", simplePage.getCountTotal());
        result.put("pageSize", simplePage.getPageSize());
        result.put("pageNo", simplePage.getPageNo());
        result.put("pageTotal", simplePage.getPageTotal());

        result.put("list", fileInfoVOS);
        return getSuccessResponseVO(result);
    }

    // 还原文件
    @RequestMapping("/recoverFile")
    @GlobalInterceptor(checkParam = true)
    public ResponseVO recoverFile(HttpSession session, @VerifyParam(require = true) String fileIds) {
        SessionWebUserDto user = getUserinfoFromSession(session);
        String userId = user.getUserId();
        fileInfoService.recoverFileBatch(userId, fileIds);

        return getSuccessResponseVO(null);

    }

    /**
     * 彻底删除文件
     */
    @RequestMapping("/delFile")
    @GlobalInterceptor(checkParam = true)
    public ResponseVO delFile(HttpSession session, @VerifyParam(require = true) String fileIds) {
        SessionWebUserDto user = getUserinfoFromSession(session);
        fileInfoService.thoroughDelFilesInfoByFileIdsAndUserId(fileIds, user.getUserId());
        return getSuccessResponseVO(null);
    }
}
