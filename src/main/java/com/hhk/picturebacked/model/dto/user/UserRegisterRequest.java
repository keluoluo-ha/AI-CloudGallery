package com.hhk.picturebacked.model.dto.user;

import lombok.Data;

import java.io.Serializable;



// DTO（数据传输对象）= 中间层快递盒
@Data
public class UserRegisterRequest implements Serializable {
    private static final long serialVersionUID = 3191241716373120793L;
    private String userAccount;
    private  String UserPassword;
    private  String checkPassword;
}
