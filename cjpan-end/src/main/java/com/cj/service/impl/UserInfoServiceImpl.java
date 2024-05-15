package com.cj.service.impl;

import com.cj.annotation.GlobalInterceptor;
import com.cj.annotation.VerifyParam;
import com.cj.component.RedisComponent;
import com.cj.entity.config.AppConfig;
import com.cj.entity.constants.Constants;
import com.cj.entity.dto.QQInfoDto;
import com.cj.entity.dto.SessionWebUserDto;
import com.cj.entity.dto.SysSettingsDto;
import com.cj.entity.dto.UserSpaceDto;
import com.cj.entity.enums.PageSize;
import com.cj.entity.enums.UserStatusEnum;
import com.cj.entity.enums.VerifyRegexEnum;
import com.cj.entity.po.FileInfo;
import com.cj.entity.po.UserInfo;
import com.cj.entity.query.FileInfoQuery;
import com.cj.entity.query.SimplePage;
import com.cj.entity.query.UserInfoQuery;
import com.cj.entity.vo.PaginationResultVO;
import com.cj.entity.vo.ResponseVO;
import com.cj.exception.BusinessException;
import com.cj.mappers.FileInfoMapper;
import com.cj.mappers.UserInfoMapper;
import com.cj.service.EmailCodeService;
import com.cj.service.UserInfoService;
import com.cj.utils.JsonUtils;
import com.cj.utils.OKHttpUtils;
import com.cj.utils.RedisUtils;
import com.cj.utils.StringTools;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;


/**
 * 用户信息 业务接口实现
 */
@Service("userInfoService")
public class UserInfoServiceImpl implements UserInfoService {

    @Resource
    private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

    @Resource
    private EmailCodeService emailCodeService;

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private AppConfig appConfig;

    @Resource
    private FileInfoMapper<FileInfo, FileInfoQuery> fileInfoMapper;

    private static final Logger logger = LoggerFactory.getLogger(UserInfoServiceImpl.class);
    /**
     * 根据条件查询列表
     */
    @Override
    public List<UserInfo> findListByParam(UserInfoQuery param) {
        return this.userInfoMapper.selectList(param);
    }

    /**
     * 根据条件查询列表
     */
    @Override
    public Integer findCountByParam(UserInfoQuery param) {
        return this.userInfoMapper.selectCount(param);
    }

    /**
     * 分页查询方法
     */
    @Override
    public PaginationResultVO<UserInfo> findListByPage(UserInfoQuery param) {
        int count = this.findCountByParam(param);
        int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

        SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
        param.setSimplePage(page);
        List<UserInfo> list = this.findListByParam(param);
        PaginationResultVO<UserInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
        return result;
    }

    /**
     * 新增
     */
    @Override
    public Integer add(UserInfo bean) {
        return this.userInfoMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    @Override
    public Integer addBatch(List<UserInfo> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.userInfoMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或者修改
     */
    @Override
    public Integer addOrUpdateBatch(List<UserInfo> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.userInfoMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 根据UserId获取对象
     */
    @Override
    public UserInfo getUserInfoByUserId(String userId) {
        return this.userInfoMapper.selectByUserId(userId);
    }

    /**
     * 根据UserId修改
     */
    @Override
    public Integer updateUserInfoByUserId(UserInfo bean, String userId) {
        return this.userInfoMapper.updateByUserId(bean, userId);
    }

    /**
     * 根据UserId删除
     */
    @Override
    public Integer deleteUserInfoByUserId(String userId) {
        return this.userInfoMapper.deleteByUserId(userId);
    }

    /**
     * 根据Email获取对象
     */
    @Override
    public UserInfo getUserInfoByEmail(String email) {
        return this.userInfoMapper.selectByEmail(email);
    }

    /**
     * 根据Email修改
     */
    @Override
    public Integer updateUserInfoByEmail(UserInfo bean, String email) {
        return this.userInfoMapper.updateByEmail(bean, email);
    }

    /**
     * 根据Email删除
     */
    @Override
    public Integer deleteUserInfoByEmail(String email) {
        return this.userInfoMapper.deleteByEmail(email);
    }

    /**
     * 根据NickName获取对象
     */
    @Override
    public UserInfo getUserInfoByNickName(String nickName) {
        return this.userInfoMapper.selectByNickName(nickName);
    }

    /**
     * 根据NickName修改
     */
    @Override
    public Integer updateUserInfoByNickName(UserInfo bean, String nickName) {
        return this.userInfoMapper.updateByNickName(bean, nickName);
    }

    /**
     * 根据NickName删除
     */
    @Override
    public Integer deleteUserInfoByNickName(String nickName) {
        return this.userInfoMapper.deleteByNickName(nickName);
    }

    /**
     * 根据QqOpenId获取对象
     */
    @Override
    public UserInfo getUserInfoByQqOpenId(String qqOpenId) {
        return this.userInfoMapper.selectByQqOpenId(qqOpenId);
    }

    /**
     * 根据QqOpenId修改
     */
    @Override
    public Integer updateUserInfoByQqOpenId(UserInfo bean, String qqOpenId) {
        return this.userInfoMapper.updateByQqOpenId(bean, qqOpenId);
    }

    /**
     * 根据QqOpenId删除
     */
    @Override
    public Integer deleteUserInfoByQqOpenId(String qqOpenId) {
        return this.userInfoMapper.deleteByQqOpenId(qqOpenId);
    }

    /**
     * 用户注册
     * @param email
     * @param nickName
     * @param password
     * @param emailCode
     */
    @Override
    public void register(String email, String nickName, String password, String emailCode) {
        UserInfo userInfo = userInfoMapper.selectByEmail(email);
        if(null != userInfo){
            throw new BusinessException("邮箱账号已经存在......");
        }
        UserInfo nickNameUser = this.userInfoMapper.selectByNickName(nickName);
        if(null != nickNameUser){
            throw new BusinessException("用户名已存在.....");
        }
        // 邮箱验证码
        emailCodeService.checkCode(email, emailCode);
        userInfo = new UserInfo();
        String uuid = UUID.randomUUID().toString().replace("-", "");
        userInfo.setUserId(uuid);
        userInfo.setEmail(email);
        userInfo.setNickName(nickName);
        userInfo.setPassword(StringTools.encodeByMD5(password));
        userInfo.setJoinTime(new Date());
        userInfo.setStatus(Constants.ONE);
        userInfo.setUseSpace(0L);
        SysSettingsDto sysSettingsDto = redisComponent.getSysSettingsDto(email);
        userInfo.setTotalSpace(sysSettingsDto.getUserInitUseSpace() * Constants.MB);
        userInfoMapper.insert(userInfo);
    }

    @Override
    public void resetPwd(String email, String password, String emailCode) {
        emailCodeService.checkCode(email, emailCode);
        UserInfo userInfo = userInfoMapper.selectByEmail(email);
        if(null == userInfo){
            throw new BusinessException("用户不存在");
        }
        if(password.equals(userInfo.getPassword())){
            throw new BusinessException("修改的密码和原密码一致.");
        }
        if(userInfo.getStatus().equals(UserStatusEnum.DISABLE.getStatus())){
            throw new BusinessException("用户已被禁用，请及时联系官方");
        }
        UserInfo userUpdateInfo = new UserInfo();
        userUpdateInfo.setPassword(StringTools.encodeByMD5(password));
        userInfoMapper.updateByUserId(userUpdateInfo,userInfo.getUserId());
    }

    @Override
    public void updateUserStatus(String userId, Integer status) {

    }

    @Override
    public void changeUserSpace(String userId, Integer changeSpace) {

    }

    @Override
    public SessionWebUserDto login(String email, String password) {
        UserInfo userInfo = userInfoMapper.selectByEmail(email);

        if (null == userInfo || !userInfo.getPassword().equals(password)){
            System.out.println(userInfo.getPassword());
            System.out.println("输入的密码是：" + password);
            throw new BusinessException("账号或者密码错误");
        }

        if(userInfo.getStatus().equals(UserStatusEnum.DISABLE.getStatus())){
            throw new BusinessException("账号已禁用");
        }
        UserInfo userUpdateInfo = new UserInfo();
        userUpdateInfo.setLastLoginTime(new Date());
        userInfoMapper.updateByUserId(userUpdateInfo, userInfo.getUserId());
        SessionWebUserDto userDto = new SessionWebUserDto();
        userDto.setUserId(userInfo.getUserId());
        userDto.setNickName(userInfo.getNickName());
        if (ArrayUtils.contains(appConfig.getAdminEmails().split(","), email)){
            userDto.setAdmin(true);
        }else {
            userDto.setAdmin(false);
        }
        // 用户空间
        UserSpaceDto userSpaceDto = new UserSpaceDto();
        Long userspace = fileInfoMapper.selectUserSpaceByUserId(userInfo.getUserId());
        userSpaceDto.setUseSpace(userspace);
        userSpaceDto.setTotalSpace(userInfo.getTotalSpace());
        redisComponent.saveUserSpaceUse(userInfo.getUserId(), userSpaceDto);
        return userDto;
    }

    @Override
    public SessionWebUserDto qqLogin(String code) {
        String accessToken = getQQAccessToken(code);
        String openId = getQQOpenId(accessToken);
        UserInfo user = this.userInfoMapper.selectByQqOpenId(openId);
        String avatar = null;
        if (null == user) {
            QQInfoDto qqInfo = getQQUserInfo(accessToken, openId);
            user = new UserInfo();

            String nickName = qqInfo.getNickname();
            nickName = nickName.length() > Constants.LENGTH_150 ? nickName.substring(0, 150) : nickName;
            avatar = StringTools.isEmpty(qqInfo.getFigureurl_qq_2()) ? qqInfo.getFigureurl_qq_1() : qqInfo.getFigureurl_qq_2();
            Date curDate = new Date();

            //上传头像到本地
            user.setQqOpenId(openId);
            user.setJoinTime(curDate);
            user.setNickName(nickName);
            user.setQqAvatar(avatar);
            user.setUserId(StringTools.getRandomString(Constants.LENGTH_10));
            user.setLastLoginTime(curDate);
            user.setStatus(UserStatusEnum.ENABLE.getStatus());
            user.setUseSpace(0L);
            user.setTotalSpace(redisComponent.getSysSettingsDto().getUserInitUseSpace() * Constants.MB);
            this.userInfoMapper.insert(user);
            user = userInfoMapper.selectByQqOpenId(openId);
        } else {
            UserInfo updateInfo = new UserInfo();
            updateInfo.setLastLoginTime(new Date());
            avatar = user.getQqAvatar();
            this.userInfoMapper.updateByQqOpenId(updateInfo, openId);
        }
        if (UserStatusEnum.DISABLE.getStatus().equals(user.getStatus())) {
            throw new BusinessException("账号被禁用无法登录");
        }
        SessionWebUserDto sessionWebUserDto = new SessionWebUserDto();
        sessionWebUserDto.setUserId(user.getUserId());
        sessionWebUserDto.setNickName(user.getNickName());
        sessionWebUserDto.setAvatar(avatar);
        if (ArrayUtils.contains(appConfig.getAdminEmails().split(","), user.getEmail() == null ? "" : user.getEmail())) {
            sessionWebUserDto.setAdmin(true);
        } else {
            sessionWebUserDto.setAdmin(false);
        }

        UserSpaceDto userSpaceDto = new UserSpaceDto();
//        userSpaceDto.setUseSpace(fileInfoService.getUserUseSpace(user.getUserId()));
        Long userSpace = fileInfoMapper.selectUserSpaceByUserId(user.getUserId());
        userSpaceDto.setUseSpace(userSpace);
        userSpaceDto.setTotalSpace(user.getTotalSpace());
        redisComponent.saveUserSpaceUse(user.getUserId(), userSpaceDto);
        return sessionWebUserDto;
    }
    private String getQQAccessToken(String code) {
        /**
         * 返回结果是字符串 access_token=*&expires_in=7776000&refresh_token=* 返回错误 callback({UcWebConstants.VIEW_OBJ_RESULT_KEY:111,error_description:"error msg"})
         */
        String accessToken = null;
        String url = null;
        try {
            url = String.format(appConfig.getQqUrlAccessToken(), appConfig.getQqAppId(), appConfig.getQqAppKey(), code, URLEncoder.encode(appConfig
                    .getQqUrlRedirect(), "utf-8"));
        } catch (UnsupportedEncodingException e) {
            logger.error("encode失败");
        }
        String tokenResult = OKHttpUtils.getRequest(url);
        if (tokenResult == null || tokenResult.contains(Constants.VIEW_OBJ_RESULT_KEY)) {
            logger.error("获取qqToken失败:{}", tokenResult);
            throw new BusinessException("获取qqToken失败");
        }
        String[] params = tokenResult.split("&");
        if (params != null && params.length > 0) {
            for (String p : params) {
                if (p.indexOf("access_token") != -1) {
                    accessToken = p.split("=")[1];
                    break;
                }
            }
        }
        return accessToken;
    }


    private String getQQOpenId(String accessToken) throws BusinessException {
        // 获取openId
        String url = String.format(appConfig.getQqUrlOpenid(), accessToken);
        String openIDResult = OKHttpUtils.getRequest(url);
        String tmpJson = this.getQQResp(openIDResult);
        if (tmpJson == null) {
            logger.error("调qq接口获取openID失败:tmpJson{}", tmpJson);
            throw new BusinessException("调qq接口获取openID失败");
        }
        Map jsonData = JsonUtils.convertJson2Obj(tmpJson, Map.class);
        if (jsonData == null || jsonData.containsKey(Constants.VIEW_OBJ_RESULT_KEY)) {
            logger.error("调qq接口获取openID失败:{}", jsonData);
            throw new BusinessException("调qq接口获取openID失败");
        }
        return String.valueOf(jsonData.get("openid"));
    }


    private QQInfoDto getQQUserInfo(String accessToken, String qqOpenId) throws BusinessException {
        String url = String.format(appConfig.getQqUrlUserInfo(), accessToken, appConfig.getQqAppId(), qqOpenId);
        String response = OKHttpUtils.getRequest(url);
        if (StringUtils.isNotBlank(response)) {
            QQInfoDto qqInfo = JsonUtils.convertJson2Obj(response, QQInfoDto.class);
            if (qqInfo.getRet() != 0) {
                logger.error("qqInfo:{}", response);
                throw new BusinessException("调qq接口获取用户信息异常");
            }
            return qqInfo;
        }
        throw new BusinessException("调qq接口获取用户信息异常");
    }

    private String getQQResp(String result) {
        if (StringUtils.isNotBlank(result)) {
            int pos = result.indexOf("callback");
            if (pos != -1) {
                int start = result.indexOf("(");
                int end = result.lastIndexOf(")");
                String jsonStr = result.substring(start + 1, end - 1);
                return jsonStr;
            }
        }
        return null;
    }

}