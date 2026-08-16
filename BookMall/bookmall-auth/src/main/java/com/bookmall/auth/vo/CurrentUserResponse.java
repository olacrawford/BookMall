package com.bookmall.auth.vo;

import lombok.Data;

@Data
public class CurrentUserResponse {

    private Long userId;
    private String username;
    private String nickname;

    public CurrentUserResponse() {
    }

    public CurrentUserResponse(Long userId, String username, String nickname) {
        this.userId = userId;
        this.username = username;
        this.nickname = nickname;
    }

}