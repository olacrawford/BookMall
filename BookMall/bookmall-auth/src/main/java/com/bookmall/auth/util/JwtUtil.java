package com.bookmall.auth.util;

import com.bookmall.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

//生成Token和解析Token
public class JwtUtil {

    //密钥和过期时间
    private final SecretKey secretKey;
    private final long expireSeconds;

    public JwtUtil(String secret, long expireSeconds) {
        //密码学算法最终处理的是字节
        //把你yml配置里的普通字符串密钥（secret），转换成JWT签名所需要的SecretKey加密密钥对象
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireSeconds = expireSeconds;
    }

    //生成Token
    public String generateToken(User user) {
        //获取当前时间
        Date now = new Date();
        //获取过期时间
        Date expireAt = new Date(now.getTime() + expireSeconds * 1000);

        //JWT=头部：算法，载荷：存你的 userId、username，签发时间；签名。
        return Jwts.builder()
                .setSubject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .claim("nickname", user.getNickname())
                .setIssuedAt(now)
                .setExpiration(expireAt)
                //使用密钥给JWT签名
                .signWith(secretKey)
                //拼一起
                .compact();
    }

    //解析 JWT
    //Claims 可以理解成：JWT 里面存放的数据集合
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

}