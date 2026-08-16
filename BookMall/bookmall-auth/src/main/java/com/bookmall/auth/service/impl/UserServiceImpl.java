package com.bookmall.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookmall.auth.dto.LoginRequest;
import com.bookmall.auth.dto.RegisterRequest;
import com.bookmall.auth.entity.User;
import com.bookmall.auth.mapper.UserMapper;
import com.bookmall.auth.service.UserService;
import com.bookmall.auth.util.JwtUtil;
import com.bookmall.auth.vo.CurrentUserResponse;
import com.bookmall.auth.vo.LoginResponse;
import com.bookmall.auth.vo.UserVO;
import com.bookmall.common.exception.BusinessException;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtUtil jwtUtil;

    public UserServiceImpl(UserMapper userMapper,
                           @Value("${jwt.secret}") String secret,
                           @Value("${jwt.expire-seconds}") long expireSeconds) {
        this.userMapper = userMapper;
        this.jwtUtil = new JwtUtil(secret, expireSeconds);
    }

    @Override
    public UserVO register(RegisterRequest request) {
        User exists = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (exists != null) {
            throw new BusinessException(400, "用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        // 认证服务只保存密码密文，避免明文落库。
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        userMapper.insert(user);
        return new UserVO(user.getId(), user.getUsername(), user.getNickname(), user.getPhone(), user.getEmail());
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (user == null) {
            throw new BusinessException(400, "用户不存在");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(400, "密码错误");
        }

        String token = jwtUtil.generateToken(user);
        return new LoginResponse(token, user);
    }

    @Override
    public CurrentUserResponse currentUser(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(400, "token不能为空");
        }

        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // /me 直接从 JWT 解析当前用户，避免再次查库。
        Claims claims = jwtUtil.parseToken(token);
        Long userId = Long.valueOf(claims.getSubject());
        String username = (String) claims.get("username");
        String nickname = (String) claims.get("nickname");

        return new CurrentUserResponse(userId, username, nickname);
    }
}
