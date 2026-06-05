package com.hhk.picturebacked.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;
//Entity（实体）= 数据库表的映射
@Getter
public enum UserRoleEnums {

    USER("用户","user"),
    ADMIN("管理员","admin");

    private final String text;
    private final String value;
    UserRoleEnums(String text,String value){
        this.value=value;
        this.text=text;
    }
    public static UserRoleEnums getEnumByValue(String value){
        if(ObjUtil.isEmpty(value)){
            return null;
        }
        for(UserRoleEnums anEnum :UserRoleEnums.values()){
         if(anEnum.value.equals(value)){
             return anEnum;
         }
        }
        return null;
    }
}
