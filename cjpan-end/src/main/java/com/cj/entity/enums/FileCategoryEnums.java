package com.cj.entity.enums;


import lombok.Getter;

@Getter
public enum FileCategoryEnums {
    // 视频 音频 图片 文档 其他
    VIDEO (1,"video","视频"),
    MUSIC(2,"music","音频"),
    IMAGE(3,"image","图片"),
    DOC(4,"doc","文档"),
    OTHERS(5,"others","其他");
    private Integer category;
    private String code;
    private String desc;

    FileCategoryEnums() {
    }

    FileCategoryEnums(Integer category, String code, String desc) {
        this.category = category;
        this.code = code;
        this.desc = desc;
    }
    public static FileCategoryEnums getCode(String code){
        for(FileCategoryEnums fileCategoryEnums : FileCategoryEnums.values()){
            if(code.equals(fileCategoryEnums.code)){
                return fileCategoryEnums;
            }
        }
        return null;
    }
}
