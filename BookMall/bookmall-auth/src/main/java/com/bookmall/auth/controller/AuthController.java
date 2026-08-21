package com.bookmall.auth.controller;

import com.bookmall.auth.dto.LoginRequest;
import com.bookmall.auth.dto.RegisterRequest;
import com.bookmall.auth.service.UserService;
import com.bookmall.auth.vo.LoginResponse;
import com.bookmall.auth.vo.UserVO;
import com.bookmall.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    // 用final避免userService被重新赋值
    private final UserService userService;

    //依赖注入
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    //测试服务是否正常
    @GetMapping("/hello")
    public Result<String> hello() {
        return Result.success("bookmall-auth is running");
    }

    //用户注册
    @PostMapping("/register")
    //@Valid 开启参数校验
    //@RequestBody 把前端请求体里面的JSON，反序列化为Java对象RegisterRequest request
    public Result<UserVO> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(userService.register(request));
    }

    //用户登录
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(userService.login(request));
    }

}