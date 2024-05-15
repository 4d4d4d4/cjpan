package com.cj.aop;

import com.cj.annotation.GlobalInterceptor;
import com.cj.annotation.VerifyParam;
import com.cj.entity.constants.Constants;
import com.cj.entity.dto.SessionWebUserDto;
import com.cj.entity.enums.ResponseCodeEnum;
import com.cj.exception.BusinessException;
import com.cj.utils.StringTools;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.cj.utils.VerifyUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.jar.Attributes;

/**
 * @Classname AutoCheckParamAspect
 * @Description 什么也没有写哦~
 * @Date 2024/3/5 8:28
 * @Created by 憧憬
 */
@Aspect
@Component
public class AutoCheckParamAspect {
    private static final Logger logger = LoggerFactory.getLogger(AutoCheckParamAspect.class);
    private static final String TYPE_STRING = "java.lang.String";
    private static final String TYPE_INTEGER = "java.lang.Integer";
    private static final String TYPE_LONG = "java.lang.Long";
    private static final String TYPE_boolean = "boolean";

    // 切点 表明切面类切入的规则
    @Pointcut("@annotation(com.cj.annotation.GlobalInterceptor)")
    public void requestInterceptor() {
    }


    @Before("requestInterceptor()")
    public void interceptorDo(JoinPoint point) {
        try {
            Object o = point.getTarget(); // 获取目标代理对象 --> Controller
            Signature signature = point.getSignature(); // 获取签名
            Object[] args = point.getArgs(); // 获取参数内容
            String name = signature.getName(); // 方法名称
            Class<?>[] parameterTypes = ((MethodSignature) point.getSignature()).getMethod().getParameterTypes(); // 参数类型
            GlobalInterceptor annotation = ((MethodSignature) point.getSignature()).getMethod().getAnnotation(GlobalInterceptor.class); // 判断方法是否有全局注解
            Method method = ((MethodSignature) point.getSignature()).getMethod(); // 获取方法 --> controller中的方法
            Parameter[] parameters = method.getParameters();  // 获取参数信息
            if(null == annotation){
                return;
            }
            /**
             * 校验登录
             */
            if(annotation.checkLogin()){
                checkLogin(annotation.checkAdmin());
            }
            /*
             * 校验参数
             */
            if (annotation.checkParam()){
                // 传入方法 参数值
                validateParams(method, args);
            }
//            System.out.println("1");
        }catch(Exception e){
            logger.error("参数校验失败....");
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * 校验登录
     * @param isAdmin 是否为管理员
     */
    private void checkLogin(boolean isAdmin) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        HttpSession session = request.getSession();
        SessionWebUserDto userDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        if(null == userDto){
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }
        if(isAdmin && !userDto.getAdmin()){
            throw new BusinessException(ResponseCodeEnum.CODE_404);
        }

    }

    /**
     * 校验参数
     * @param method 参数的方法
     * @param args 参数值
     */
    private void validateParams(Method method, Object[] args) {
        Parameter[] parameters = method.getParameters(); // 获取方法中的参数的信息
        for(int i = 0; i < parameters.length; i++){
            Parameter parameter = parameters[i];
            // 查看该参数是否有VerifyParam注解 有注解的才进行参数校验
            VerifyParam annotation = parameter.getAnnotation(VerifyParam.class);
            Object value = args[i];
            if(null == annotation){
                // 如果没有标注参数检验注解则跳过
                continue;
            }
            String className = parameter.getParameterizedType().getTypeName();
            if(className.equals(TYPE_STRING) || className.equals(TYPE_LONG) || className.equals(TYPE_INTEGER) || className.equals(TYPE_boolean)){
                // 参数的类型为基本类型
                checkBasicValue(parameter, value);
            }else {
                // 参数为对象
                checkObjectValue(parameter,value);
            }
        }
    }

    /**
     * 校验对象类型参数
     * @param parameter 参数信息
     * @param value 参数值
     */
    private void checkObjectValue(Parameter parameter, Object value) {
        try {
            String paramTypeName = parameter.getParameterizedType().getTypeName(); // 通过参数信息 获取参数对象名称
            System.out.println("对象类型名称：" + paramTypeName);
            // 通过类名称 创建对象实列
            Class<?> aClass = Class.forName(paramTypeName); // 反射对象
            Field[] fields = aClass.getDeclaredFields(); // 获取对象中所有公共字段信息
            for(int i = 0; i < fields.length ; i++){
                Field field = fields[i];
                // 查看该字段是否标注参数校验注解
                VerifyParam annotation = field.getAnnotation(VerifyParam.class);
                if(null == annotation || !annotation.require()){
                    return;
                }
                field.setAccessible(true); // 设置访问权限 允许访问 即使它是私有的 或者 受保护的
                Object valueByFiled = field.get(value); // value对象中field字段所对应的值
                checkBasicValue(parameter, valueByFiled);

            }
        } catch (Exception e) {
            logger.error("校验对象类型参数错误");
            throw new RuntimeException(e);
        }
    }

    /**
     * 校验基本类型参数
     * @param parameter 参数信息
     * @param value 参数值
     */
    private void checkBasicValue(Parameter parameter, Object value) {
        VerifyParam annotation = parameter.getAnnotation(VerifyParam.class);
        Boolean isEmpty = value == null || StringTools.isEmpty(value.toString());
        Integer length = value == null ? 0 : value.toString().length();
        // 如果需要检验参数不为空 如果需要检验参数长度 如果需要检验参数是否符合正则表达式
        if(annotation.require() && isEmpty){
            // 参数不应为空 但值为空
            throw new BusinessException(ResponseCodeEnum.CODE_600 + "1");
        }
        if(!isEmpty && ((annotation.min() != -1 && length < annotation.min()) || (annotation.max() != -1 && length > annotation.max()))){
            // 字符串不为空 且 长度不符合规范
            throw new BusinessException(ResponseCodeEnum.CODE_600 + "2");
        }
        if(!isEmpty && !StringTools.isEmpty(annotation.regex().getRegex()) && !VerifyUtils.verify(annotation.regex().getRegex(), value.toString())){
            // 字符串为空 且 字符串需要正则校验 且 校验失败
            throw new BusinessException(ResponseCodeEnum.CODE_600 + "3");
        }
    }
}

