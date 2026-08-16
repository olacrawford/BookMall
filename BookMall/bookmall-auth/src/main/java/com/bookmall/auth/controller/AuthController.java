package com.bookmall.auth.controller;

import com.bookmall.auth.dto.LoginRequest;
import com.bookmall.auth.dto.RegisterRequest;
import com.bookmall.auth.service.UserService;
import com.bookmall.auth.vo.CurrentUserResponse;
import com.bookmall.auth.vo.LoginResponse;
import com.bookmall.auth.vo.UserVO;
import com.bookmall.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    // 调用UserService对象，final这个变量一旦赋值，以后不能再指向另外一个对象
    // 用final避免userService被重新赋值
    private final UserService userService;

    //依赖注入
    //当Bean类仅有一个构造方法时，Spring 会自动使用这个构造做依赖注入，不需要写@Autowired
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

    //获取当前登录的用户
    @GetMapping("/me")
    //读取 HTTP 请求头的数据，拿请求头里叫Authorization的值，这个地方前端放 token 令牌
    //@required = false：Authorization 请求头可以没有
    public Result<CurrentUserResponse> me(@RequestHeader(value = "Authorization", required = false) String token) {
        return Result.success(userService.currentUser(token));
    }

}