package com.cj.entity.constants;

import org.springframework.boot.system.ApplicationHome;

import java.io.File;

public class Constants {
    private static final ApplicationHome  applicationHome = new ApplicationHome(Constants.class);
    public static final String ZERO_STR = "0";

    public static final Integer ZERO = 0;

    public static final Integer ONE = 1;

    public static final Integer LENGTH_30 = 30;

    public static final Integer LENGTH_10 = 10;

    public static final Integer LENGTH_20 = 20;

    public static final Integer LENGTH_5 = 5;

    public static final Integer LENGTH_15 = 15;

    public static final Integer LENGTH_150 = 150;

    public static final Integer LENGTH_50 = 50;

    public static final String SESSION_KEY = "session_key";

    public static final String SESSION_SHARE_KEY = "session_share_key_";

    public static final String FILE_FOLDER_FILE = "file/";
    public static final String FILE_FOLDER_LOGS = "/logs/";
    public static final String STATIC_ASSET_PATH = applicationHome.getDir().getAbsolutePath() + File.separator + "static" + File.separator;
    public static final String FILE_FOLDER_TEMP = "temp/";

    public static final String IMAGE_PNG_SUFFIX = ".png";

    public static final String TS_NAME = "index.ts";

    public static final String M3U8_NAME = "index.m3u8";

    public static final String CHECK_CODE_KEY = "check_code_key";

    public static final String CHECK_CODE_KEY_EMAIL = "check_code_key_email";

    public static final String AVATAR_SUFFIX = ".jpg";

    public static final String FILE_FOLDER_AVATAR_NAME = "avatar/";

    public static final String AVATAR_DEFAULT = "default_avatar.jpg";

    public static final String VIEW_OBJ_RESULT_KEY = "result";

    /**
     * redis key 相关
     */
    /**
     * QQ邮箱键
     */
    public static final String REDIS_QQ_EMAIL_KEY = "cjpan:QQEmail:";
    /**
     * 过期时间 1分钟
     */
    public static final Integer REDIS_KEY_EXPIRES_ONE_MIN = 60;
    /**
     * 过期时间 1小时
     */
    public static final Integer REDIS_KEY_EXPIRES_ONE_HOUR = 60 * 60;

    /**
     * 过期时间 1天
     */
    public static final Integer REDIS_KEY_EXPIRES_DAY = REDIS_KEY_EXPIRES_ONE_MIN * 60 * 24;


    public static final Long MB = 1024 * 1024L;

    /**
     * 过期时间5分钟
     */
    public static final Integer REDIS_KEY_EXPIRES_FIVE_MIN = REDIS_KEY_EXPIRES_ONE_MIN * 5;

    public static final Integer REDIS_KEY_EXPIRES_FIFTEEN_MIN = REDIS_KEY_EXPIRES_ONE_MIN * 15;
    public static final String REDIS_KEY_DOWNLOAD = "cj:download:";

    public static final String REDIS_KEY_SYS_SETTING = "cj:syssetting:";

    public static final String REDIS_KEY_USER_SPACE_USE = "cj:user:spaceuse:";

    public static final String REDIS_KEY_USER_FILE_TEMP_SIZE = "cj:user:file:temp:";

}
