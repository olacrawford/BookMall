package com.bookmall.auth.service;

import com.bookmall.auth.dto.LoginRequest;
import com.bookmall.auth.dto.RegisterRequest;
import com.bookmall.auth.vo.LoginResponse;
import com.bookmall.auth.vo.UserVO;

public interface UserService {

    UserVO register(RegisterRequest request);

    LoginResponse login(LoginRequest request);
}
