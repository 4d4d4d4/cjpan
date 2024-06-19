package com.cj.controller;

import com.cj.annotation.GlobalInterceptor;
import com.cj.annotation.VerifyParam;
import com.cj.entity.dto.SessionWebUserDto;
import com.cj.entity.po.FileShare;
import com.cj.entity.query.FileShareQuery;
import com.cj.entity.vo.PaginationResultVO;
import com.cj.entity.vo.ResponseVO;
import com.cj.service.FileShareService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

/**
 * @Classname ShowShareController
 * @Description 分享文件接口
 * @Date 2024/6/5 下午5:32
 * @Created by 憧憬
 */

@RestController("shareController")
@RequestMapping("/share")
public class ShareController extends ABaseController {
    @Resource
    private FileShareService fileShareService;

    /**
     * 查询所有分享文件列表
     * @param session session
     * @param query 查询条件 同文件查询差不多
     * @return
     */
    @RequestMapping("/loadShareList")
    @GlobalInterceptor(checkParam = true)
    public ResponseVO loadShareList(HttpSession session, FileShareQuery query) {
        query.setOrderBy("share_time desc");
        SessionWebUserDto userDto = getUserinfoFromSession(session);
        query.setUserId(userDto.getUserId());
        query.setQueryFileName(true);
        PaginationResultVO resultVO = this.fileShareService.findListByPage(query);
        return getSuccessResponseVO(resultVO);
    }

    /**
     * 分享某个文件
     * @param session session
     * @param fileId 被分享文件的id
     * @param validType 有效期 0:1天 1:7天 2:30天 3:永久有效
     * @param code 分享验证码
     * @return
     */
    @RequestMapping("/shareFile")
    @GlobalInterceptor(checkParam = true )
    public ResponseVO shareFile(HttpSession session,
                                @VerifyParam(require = true) String fileId,
                                @VerifyParam(require = true) Integer validType,
                                String code) {
        SessionWebUserDto userDto = getUserinfoFromSession(session);
        FileShare share = new FileShare();
        share.setFileId(fileId);
        share.setValidType(validType);
        share.setCode(code);
        share.setUserId(userDto.getUserId());
        fileShareService.saveShare(share);
        return getSuccessResponseVO(share);
    }

    /**
     * 取消分享
     * @param session session
     * @param shareIds 被取消分享的文件id
     * @return
     */
    @RequestMapping("/cancelShare")
    @GlobalInterceptor(checkParam = true)
    public ResponseVO cancelShare(HttpSession session, @VerifyParam(require = true) String shareIds) {
        SessionWebUserDto userDto = getUserinfoFromSession(session);
        fileShareService.deleteFileShareBatch(shareIds.split(","), userDto.getUserId());
        return getSuccessResponseVO(null);
    }
}
