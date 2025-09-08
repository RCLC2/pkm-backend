package com.ns.user.user.vo;

public record RefreshTokenVo(String userId, String refreshToken) {
    public static RefreshTokenVo of(String userId, String refreshToken){
        return new RefreshTokenVo(userId,refreshToken);
    }
}

