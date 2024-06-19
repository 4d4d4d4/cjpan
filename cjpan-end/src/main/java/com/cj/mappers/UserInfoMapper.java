package com.cj.mappers;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 用户信息 数据库操作接口
 */
public interface UserInfoMapper<T, P> extends BaseMapper<T, P> {

    /**
     * 根据UserId更新
     */
    Integer updateByUserId(@Param("bean") T t, @Param("userId") String userId);


    /**
     * 根据UserId删除
     */
    Integer deleteByUserId(@Param("userId") String userId);


    /**
     * 根据UserId获取对象
     */
    T selectByUserId(@Param("userId") String userId);


    /**
     * 根据Email更新
     */
    Integer updateByEmail(@Param("bean") T t, @Param("email") String email);


    /**
     * 根据Email删除
     */
    Integer deleteByEmail(@Param("email") String email);


    /**
     * 根据Email获取对象
     */
    T selectByEmail(@Param("email") String email);


    /**
     * 根据NickName更新
     */
    Integer updateByNickName(@Param("bean") T t, @Param("nickName") String nickName);


    /**
     * 根据NickName删除
     */
    Integer deleteByNickName(@Param("nickName") String nickName);


    /**
     * 根据NickName获取对象
     */
    T selectByNickName(@Param("nickName") String nickName);


    /**
     * 根据QqOpenId更新
     */
    Integer updateByQqOpenId(@Param("bean") T t, @Param("qqOpenId") String qqOpenId);


    /**
     * 根据QqOpenId删除
     */
    Integer deleteByQqOpenId(@Param("qqOpenId") String qqOpenId);


    /**
     * 根据QqOpenId获取对象
     */
    T selectByQqOpenId(@Param("qqOpenId") String qqOpenId);


    Integer updateUserSpace(@Param("userId") String userId, @Param("useSpace") Long useSpace, @Param("totalSpace") Long totalSpace);

    // 从原有的基础上修改用户的空间
    @Update("update user_info set status = #{status} where user_id = #{userId}")
    void updateUserStatus(@Param("userId") String userId, @Param("status") Integer status);

    // 修改用户总空间
    @Update("update user_info set total_space = #{changeSpace} where user_id = #{userId} and use_space <= #{changeSpace}")
    void changeUserTotalSpace(@Param("userId") String userId, @Param("changeSpace") Long changeSpace);

    // 登录时重置用户使用空间
    @Update("update user_info set use_space = #{useSpace} where user_id = #{userId} and #{useSpace} <= total_space")
    void ResetUseSpace(@Param("userId") String userId, @Param("useSpace") Long useSpace);
}
