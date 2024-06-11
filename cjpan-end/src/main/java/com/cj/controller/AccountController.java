package com.cj.controller;

import com.cj.annotation.GlobalInterceptor;
import com.cj.annotation.VerifyParam;
import com.cj.component.RedisComponent;
import com.cj.entity.config.AppConfig;
import com.cj.entity.constants.Constants;
import com.cj.entity.dto.CreateImageCode;
import com.cj.entity.dto.SessionWebUserDto;
import com.cj.entity.enums.VerifyRegexEnum;
import com.cj.entity.po.UserInfo;
import com.cj.entity.vo.ResponseVO;
import com.cj.exception.BusinessException;
import com.cj.service.EmailCodeService;
import com.cj.service.UserInfoService;
import com.cj.utils.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

@RestController("accountController")
public class AccountController extends ABaseController {
    @Resource
    private UserInfoService userInfoService;

    @Resource
    private EmailCodeService emailCodeService;

    @Resource
    private AppConfig appConfig;
    @Resource
    private RedisComponent redisComponent;

    private static final Logger logger =  LoggerFactory.getLogger(AccountController.class);

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_VALUE = "application/json;charset=UTF-8";

    /**
     * 生成验证码
     * @param response 响应 用于携带验证码图片
     * @param session session 用于存储验证码字符串
     * @param type 类型 区分是验证码还是邮箱验证码 0:非邮箱验证码 1：邮箱验证码
     * @throws IOException ~
     */
    @RequestMapping("/checkCode")
    public void checkCode(HttpServletResponse response, HttpSession session, Integer type) throws IOException {
        CreateImageCode createImageCode = new CreateImageCode(130, 30, 5, 10);
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires",0);
        response.setContentType("image/jpeg");
        String code = createImageCode.getCode();
        if(type == null || type == 0){
            session.setAttribute(Constants.CHECK_CODE_KEY, code);
        }else {
            session.setAttribute(Constants.CHECK_CODE_KEY_EMAIL, code);
        }
        createImageCode.write(response.getOutputStream());
    }

    /**
     *  发送邮箱验证码
     * @param session session 用于获取服务器存储的验证码
     * @param email 目的邮箱
     * @param checkCode 用户输入的验证码
     * @param type 0：注册 1：找回密码
     */
    @RequestMapping("/sendEmailCode")
    @GlobalInterceptor(checkParam = true, checkLogin = false) // 校验参数
    public ResponseVO sendEmailCode(HttpSession session,
                                    @VerifyParam(require = true, regex = VerifyRegexEnum.EMAIL, max = 30, min = 8) String email,
                                    @VerifyParam(require = true) String checkCode,
                                    @VerifyParam(require = true) Integer type){
        try {
            if(!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY_EMAIL))){
                throw  new BusinessException("验证码输入不正确");
            }
            emailCodeService.sendEmailCode(email, type);
            return getSuccessResponseVO(null);

        }finally {
            // 清除储存的使用邮箱所需要的图片验证码 图片验证码不管成不成都只能用一次
            session.removeAttribute(Constants.CHECK_CODE_KEY_EMAIL);
        }
    }

    /**
     *  注册账号
     * @param session 为用到
     * @param email 账号邮箱
     * @param nickName 账号昵称
     * @param password 账号密码
     * @param checkCode 图片验证码
     * @param emailCode 邮箱验证码
     */
    @RequestMapping("/register")
    @GlobalInterceptor(checkParam = true,  checkLogin = false) // 校验参数
    public ResponseVO register(HttpSession session,
                                    @VerifyParam(require = true, regex = VerifyRegexEnum.EMAIL, max = 30, min = 8) String email,
                                    @VerifyParam(require = true) String nickName,
                                    @VerifyParam(require = true, regex = VerifyRegexEnum.PASSWORD, min = 8, max = 18) String password,
                                    @VerifyParam(require = true) String checkCode,
                                    @VerifyParam(require = true) String emailCode){
        try {
            if(!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))){
                throw  new BusinessException("验证码输入不正确");
            }
            userInfoService.register(email, nickName, password, emailCode);
            return getSuccessResponseVO(null);
        }finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }

    /**
     * 登录接口
     */
    @RequestMapping("/login")
    @GlobalInterceptor(checkParam = true, checkLogin = false) // 校验参数
    public ResponseVO login(HttpSession session,
                            @VerifyParam(require = true) String email,
                            @VerifyParam(require = true) String password,
                            @VerifyParam(require = true) String checkCode,
                            @VerifyParam(require = true) boolean rememberMe){
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))){
                throw new BusinessException("图片验证码校验不一致");
            }
            SessionWebUserDto userDto = userInfoService.login(email, password);
//            if(rememberMe)
                session.setAttribute(Constants.SESSION_KEY, userDto);
            return getSuccessResponseVO(userDto);
        }finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }

    }

    /**
     * 重置密码

     * @param email 重置密码的邮箱
     * @param password 要重置的密码
     * @param checkCode 图片校验码
     * @param emailCode 邮箱校验码
     *  无返回
     */
    @RequestMapping("/resetPwd")
    @GlobalInterceptor(checkParam = true, checkLogin = false)
    public ResponseVO responseVO(HttpSession session,
                                 @VerifyParam(require = true, regex = VerifyRegexEnum.EMAIL, min = 8, max = 30) String email,
                                 @VerifyParam(require = true) String password,
                                 @VerifyParam(require = true) @RequestParam("checkCode") String checkCode,
                                 @VerifyParam(require = true) @RequestParam("emailCode") String emailCode){
        try {
            if(!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))){
                throw new BusinessException("图片验证码有误,请重新输入");
            }
            userInfoService.resetPwd(email, password, emailCode);
            return getSuccessResponseVO(null);
        }finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }

    /**
     * 退出登录
     * @param session 去掉服务器中保存的用户信息
     *  无返回
     */
    @RequestMapping("/logout")
    public ResponseVO logout(HttpSession session) {
        // 无效此会话
        session.invalidate();
        return getSuccessResponseVO(null);
    }

//    @RequestMapping("updateUserAvatar")
//    @GlobalInterceptor(checkParam = true)
//    public ResponseVO updateUserAvatar(HttpSession session,
//                                       @VerifyParam(require = true) @RequestParam("avatar") MultipartFile avatar){
//
//    }

    @RequestMapping("/updateUserAvatar")
    @GlobalInterceptor
    public ResponseVO updateUserAvatar(HttpSession session, MultipartFile avatar) {
        SessionWebUserDto webUserDto = getUserinfoFromSession(session);
        String baseFolder = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE;
        File targetFileFolder = new File(baseFolder + Constants.FILE_FOLDER_AVATAR_NAME);

        if (!targetFileFolder.exists()) {
            targetFileFolder.mkdirs();
        }
        File targetFile = new File(targetFileFolder.getPath() + "/" + webUserDto.getUserId() + Constants.AVATAR_SUFFIX);
        try {
            avatar.transferTo(targetFile);
        } catch (Exception e) {
            logger.error("上传头像失败", e);
        }

        UserInfo userInfo = new UserInfo();
        userInfo.setQqAvatar("");
        userInfoService.updateUserInfoByUserId(userInfo, webUserDto.getUserId());
        webUserDto.setAvatar(null);
        session.setAttribute(Constants.SESSION_KEY, webUserDto);
        return getSuccessResponseVO(null);
    }

    /**
     * 获取用户信息
     */
    @RequestMapping("/getUserinfo")
    public ResponseVO getUserinfo(HttpSession session){
        return getSuccessResponseVO(getUserinfoFromSession(session));
    }

    /**
     * 获取用户使用空间信息
     */
    @RequestMapping("/getUseSpace")
    public ResponseVO getUseSpace(HttpSession session) {
        SessionWebUserDto sessionWebUserDto = getUserinfoFromSession(session);
        return getSuccessResponseVO(redisComponent.getUserSpaceUse(sessionWebUserDto.getUserId()));
    }

    @RequestMapping("/getAvatar/{id}")
    @GlobalInterceptor(checkParam = true, checkLogin = false)
    public void getAvatar(HttpServletResponse response,
            @PathVariable("id") @VerifyParam(require = true) String userId){
        String avatarFolderName = Constants.FILE_FOLDER_FILE + Constants.FILE_FOLDER_AVATAR_NAME;
        // 头像所在文件夹
        String avatarFolderPath = appConfig.getProjectFolder() + avatarFolderName;
        File avatarFolderFile = new File(avatarFolderPath);
        if(!avatarFolderFile.exists()){
            avatarFolderFile.mkdirs();
        }
        String avatarPath = appConfig.getProjectFolder() + avatarFolderName + userId + Constants.AVATAR_SUFFIX;
        File avatar = new File(avatarPath);
        if(!avatar.exists()){
            if(!new File(Constants.STATIC_ASSET_PATH + Constants.AVATAR_DEFAULT).exists()){
                printNoDefaultImage(response);
            }
            avatarPath = Constants.STATIC_ASSET_PATH + Constants.AVATAR_DEFAULT;
        }
        response.setContentType("image/jpg");
        readFile(response, avatarPath);
    }
    private void printNoDefaultImage(HttpServletResponse response) {
        response.setHeader(CONTENT_TYPE, CONTENT_TYPE_VALUE);
        response.setStatus(HttpStatus.OK.value());
        try (PrintWriter writer = response.getWriter()) {
            writer.print("请在头像目录下放置默认头像default_avatar.jpg");
        } catch (Exception e) {
            logger.error("输出无默认图失败", e);
        }

    }

//    /**
//     *
//     * @param session session
//     * @param callbackUrl 登陆成功后的回调地址
//     * @return 用户信息
//     */
//    @RequestMapping("/qqlogin")
//    @GlobalInterceptor(checkParam = true, checkLogin = false)
//    public ResponseVO qqLogin(HttpSession session,
//                             @VerifyParam(require = false) String callbackUrl) throws UnsupportedEncodingException {
//
//        String state = StringTools.getRandomNumber(Constants.LENGTH_30);
//        if(!StringTools.isEmpty(state)){
//            session.setAttribute(state, callbackUrl);
//        }
//        String uri = String.format(appConfig.getQqUrlAuthorization(), appConfig.getQqAppId(), URLEncoder.encode(appConfig.getQqUrlRedirect(),"utf-8"), state);
//
//        return getSuccessResponseVO(uri);
//    }
//
//    @RequestMapping("/qqlogin/callback")
//    @GlobalInterceptor(checkParam = true, checkLogin = false)
//    public ResponseVO qqLoginCallback(HttpSession session,
//                                      @VerifyParam(require = true) String code,
//                                      @VerifyParam(require = true) String state){
//        Map<String,Object> result = new HashMap<>();
//        SessionWebUserDto userDto = new SessionWebUserDto();
//        userDto = userInfoService.qqLogin(code);
//        session.setAttribute(Constants.SESSION_KEY, userDto);
//        result.put("callbackUrl", session.getAttribute(state));
//        result.put("userInfo", userDto);
//        return getSuccessResponseVO(result);
//    }

}
