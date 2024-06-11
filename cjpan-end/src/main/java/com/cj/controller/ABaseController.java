package com.cj.controller;

import com.cj.entity.config.AppConfig;
import com.cj.entity.constants.Constants;
import com.cj.entity.dto.SessionShareDto;
import com.cj.entity.dto.SessionWebUserDto;
import com.cj.entity.enums.ResponseCodeEnum;
import com.cj.entity.vo.PaginationResultVO;
import com.cj.entity.vo.ResponseVO;
import com.cj.utils.CopyUtils;
import com.cj.utils.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;


public class ABaseController {

    private static final Logger logger = LoggerFactory.getLogger(ABaseController.class);

    protected static final String STATUC_SUCCESS = "success";

    protected static final String STATUC_ERROR = "error";

    protected <T> ResponseVO getSuccessResponseVO(T t) {
        ResponseVO<T> responseVO = new ResponseVO<>();
        responseVO.setStatus(STATUC_SUCCESS);
        responseVO.setCode(ResponseCodeEnum.CODE_200.getCode());
        responseVO.setInfo(ResponseCodeEnum.CODE_200.getMsg());
        responseVO.setData(t);
        return responseVO;
    }

    /**
     * 获取当前session中的用户信息
     * @param session
     * @return
     */
    protected SessionWebUserDto getUserinfoFromSession(HttpSession session){
       return (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
    }

    /**
     * 获取当前session中的分享信息
     * @param session
     * @return
     */
    protected SessionShareDto getShareInfoFromSession(HttpSession session, String shareId, String userId){
        System.out.println(Constants.SESSION_SHARE_KEY + shareId + getUserinfoFromSession(session).getUserId());
        return (SessionShareDto) session.getAttribute(Constants.SESSION_SHARE_KEY + shareId + "_" + userId);

    }

    /**
     * 将读取到的文件写入到输出流中
     * @param response
     * @param filePath
     */
    protected void readFile(HttpServletResponse response, String filePath){
        if(!StringTools.pathIsOk(filePath)){
            return;
        }
        FileInputStream in = null;
        OutputStream outputStream = null;
        try {
            File file = new File(filePath);
            if(!file.exists()){
                return;
            }
            in = new FileInputStream(file);
            byte[] fileByte = new byte[1024];
            int len = 0;
            outputStream = response.getOutputStream();
            while ((len = in.read(fileByte)) != -1){
                outputStream.write(fileByte, 0, len);
            }
            outputStream.flush();
        } catch (Exception e) {
            logger.error("文件读取或者写入异常");
            throw new RuntimeException(e);
        } finally {
            if(null != outputStream){
                try {
                    outputStream.close();
                } catch (IOException e) {
                    logger.error("IO错误");
                    throw new RuntimeException(e);
                }
            }
            if(null != in){
                try {
                    in.close();
                } catch (IOException e) {
                    logger.error("IO错误");
                    throw new RuntimeException(e);
                }
            }


        }
    }

    protected <S,T> PaginationResultVO<T> convert2PaginationVO(PaginationResultVO<S> source, Class<T> target){
        PaginationResultVO<T> resultVO = new PaginationResultVO<>();
        resultVO.setPageNo(source.getPageNo());
        resultVO.setPageSize(source.getPageSize());
        resultVO.setPageTotal(source.getPageTotal());
        resultVO.setTotalCount(source.getTotalCount());
        resultVO.setList(CopyUtils.copyList(source.getList(),target));
        return resultVO;
    }
}
