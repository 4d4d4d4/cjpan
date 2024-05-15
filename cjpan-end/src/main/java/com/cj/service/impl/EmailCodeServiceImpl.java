package com.cj.service.impl;

import com.cj.entity.config.AppConfig;
import com.cj.entity.constants.Constants;
import com.cj.entity.dto.SysSettingsDto;
import com.cj.entity.enums.PageSize;
import com.cj.entity.po.EmailCode;
import com.cj.entity.po.UserInfo;
import com.cj.entity.query.EmailCodeQuery;
import com.cj.entity.query.SimplePage;
import com.cj.entity.query.UserInfoQuery;
import com.cj.entity.vo.PaginationResultVO;
import com.cj.exception.BusinessException;
import com.cj.mappers.EmailCodeMapper;
import com.cj.mappers.UserInfoMapper;
import com.cj.service.EmailCodeService;
import com.cj.utils.RedisUtils;
import com.cj.utils.StringTools;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.activation.MimetypesFileTypeMap;
import javax.annotation.Resource;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.util.Date;
import java.util.List;


/**
 * 邮箱验证码 业务接口实现
 */
// cmd
@Service("emailCodeService")
public class EmailCodeServiceImpl implements EmailCodeService {

    private static final Logger logger = LoggerFactory.getLogger(EmailCodeServiceImpl.class);

    @Resource
    private EmailCodeMapper<EmailCode, EmailCodeQuery> emailCodeMapper;

    @Resource
    private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

    @Resource
    private AppConfig appConfig;

    @Resource
    private JavaMailSender javaMailSender;
    @Resource
    private RedisUtils redisUtils;

    /**
     * 根据条件查询列表
     */
    @Override
    public List<EmailCode> findListByParam(EmailCodeQuery param) {
        return this.emailCodeMapper.selectList(param);
    }

    /**
     * 根据条件查询列表
     */
    @Override
    public Integer findCountByParam(EmailCodeQuery param) {
        return this.emailCodeMapper.selectCount(param);
    }

    /**
     * 分页查询方法
     */
    @Override
    public PaginationResultVO<EmailCode> findListByPage(EmailCodeQuery param) {
        int count = this.findCountByParam(param);
        int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

        SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
        param.setSimplePage(page);
        List<EmailCode> list = this.findListByParam(param);
        PaginationResultVO<EmailCode> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
        return result;
    }

    /**
     * 新增
     */
    @Override
    public Integer add(EmailCode bean) {
        return this.emailCodeMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    @Override
    public Integer addBatch(List<EmailCode> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.emailCodeMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或者修改
     */
    @Override
    public Integer addOrUpdateBatch(List<EmailCode> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.emailCodeMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 根据EmailAndCode获取对象
     */
    @Override
    public EmailCode getEmailCodeByEmailAndCode(String email, String code) {
        return this.emailCodeMapper.selectByEmailAndCode(email, code);
    }

    /**
     * 根据EmailAndCode修改
     */
    @Override
    public Integer updateEmailCodeByEmailAndCode(EmailCode bean, String email, String code) {
        return this.emailCodeMapper.updateByEmailAndCode(bean, email, code);
    }

    /**
     * 根据EmailAndCode删除
     */
    @Override
    public Integer deleteEmailCodeByEmailAndCode(String email, String code) {
        return this.emailCodeMapper.deleteByEmailAndCode(email, code);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendEmailCode(String toEmail, Integer type) {
        if(type == Constants.ZERO){
            // 注册
            UserInfo userInfo = userInfoMapper.selectByEmail(toEmail);
            if(null != userInfo){
                throw new BusinessException("用户已存在");
            }
        }
        String code = StringTools.getRandomString(Constants.LENGTH_5);
        sendEmailCode(toEmail, code);
        // 取消之前发送的验证码的有效性
        emailCodeMapper.disableEmailCode(toEmail);

        // 保存验证码
        EmailCode emailCode = new EmailCode();
        emailCode.setEmail(toEmail);
        emailCode.setStatus(Constants.ZERO);
        emailCode.setCreateTime(new Date());
        emailCode.setCode(code);
        emailCodeMapper.insert(emailCode);

    }

    private void sendEmailCode(String toEmail, String code) {
//        if(null != redisTemplate.opsForValue().get(Constants.REDIS_QQ_EMAIL_KEY + toEmail)){
//            // 邮箱验证码未过期
//            redisUtils.remove(Constants.REDIS_QQ_EMAIL_KEY + toEmail);
//        }
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            SysSettingsDto sysSettingsDto = new SysSettingsDto();
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, true);
            messageHelper.setSubject(sysSettingsDto.getRegisterMailTitle());
            messageHelper.setFrom(appConfig.getSendEmailUsername());
            messageHelper.setTo(toEmail);
            messageHelper.setSentDate(new Date());
            messageHelper.setFileTypeMap(MimetypesFileTypeMap.getDefaultFileTypeMap());

            ApplicationHome applicationHome = new ApplicationHome(this.getClass());
            String sp = File.separator;
            String imagePath = applicationHome.getDir().getAbsolutePath() + sp + "static" + sp + RandomUtils.nextInt(1,4) + ".jpg";
//            System.out.println("---------  " + imagePath);

            // 设置内嵌照片id
            String imagePathId = "belle";
            // 添加附件
//            messageHelper.addAttachment("今日彩图", new File(imagePath));
//            String text = "<html><body><h1>憧憬网盘</h1><p>您的网盘验证码为：%s</p><img src='cid:%s'></body></html>";
//            String text = "<html><body><h1>憧憬网盘</h1><p>您的网盘验证码为：%s</p><img src='cid:%s'></body></html>";
            String text = sysSettingsDto.getRegisterEmailContent();
            text = String.format(text, code, imagePathId);
            // 添加内嵌元素（照片）
//            imagePath = "C:\\goukong.jpg";
            messageHelper.setText(text,true);
            FileSystemResource imageResource = new FileSystemResource(new File(imagePath));
            messageHelper.addInline(imagePathId, imageResource);
            sysSettingsDto.setEmailCode(code);
            redisUtils.setEx(Constants.REDIS_QQ_EMAIL_KEY + toEmail, sysSettingsDto , Constants.REDIS_KEY_EXPIRES_FIFTEEN_MIN);
            javaMailSender.send(mimeMessage);
        }catch (Exception e){
            logger.error("邮件发送失败:" , e);
            throw new BusinessException("邮件发送失败");
        }


    }


    @Override
    public void checkCode(String email, String code) {
//        EmailCode emailCode = emailCodeMapper.selectByEmailAndCode(email, code);
//        if(null == emailCode){
//            throw new BusinessException("邮箱验证码不正确");
//        }
        SysSettingsDto verityObject = (SysSettingsDto) redisUtils.get(Constants.REDIS_QQ_EMAIL_KEY + email);
        if (null == verityObject){
            throw new BusinessException("邮箱已过期,请重新发送！");
        }
        String verityCode = verityObject.getEmailCode();
        if (!verityCode.equalsIgnoreCase(code)){
            throw new BusinessException("邮箱验证码不正确");
        }
        redisUtils.remove(Constants.REDIS_QQ_EMAIL_KEY + email);
    }


}