package com.ns.user.user.vo;

public record RefreshTokenVo(String refreshToken) {
    public static RefreshTokenVo of( String refreshToken){
        return new RefreshTokenVo(refreshToken);
    }
}

