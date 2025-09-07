package com.ns.user.user.vo;

public record GoogleLoginVo(String code) {
    public static GoogleLoginVo of(String code){
        return new GoogleLoginVo(code);
    }
}
