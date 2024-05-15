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

@RestController("shareController")
@RequestMapping("/share")
public class ShareController extends ABaseController {
    @Resource
    private FileShareService fileShareService;


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

    @RequestMapping("/cancelShare")
    @GlobalInterceptor(checkParam = true)
    public ResponseVO cancelShare(HttpSession session, @VerifyParam(require = true) String shareIds) {
        SessionWebUserDto userDto = getUserinfoFromSession(session);
        fileShareService.deleteFileShareBatch(shareIds.split(","), userDto.getUserId());
        return getSuccessResponseVO(null);
    }
}
