package com.df.server.utils.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;

/**
 * JWT Token工具类
 * <p>
 * Created by lyc on 2020/3/19
 */
public class JwtTokenUtils {

    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    private static final String SECRET = "jwt@cysoft";
    private static final String ISS = "cylian";

    /**
     * 创建Token
     *
     * @param userId
     * @return
     */
    public static String createToken(Integer userId) {
        return Jwts.builder()
                .signWith(SignatureAlgorithm.HS512, SECRET)
                .setIssuer(ISS)
                .setSubject(userId.toString())
                .setIssuedAt(new Date())
                .compact();
    }

    /**
     * 从Token中获取用户Id
     *
     * @param token
     * @return
     */
    public static Integer getUserId(String token) {
        token = token.replace(JwtTokenUtils.TOKEN_PREFIX, "");
        String subject = getTokenBody(token).getSubject();
        return Integer.parseInt(subject);
    }


    /**
     * 是否已过期（由于使用Redis，不使用JWT Token自身的过期时间）
     *
     * @param token
     * @return
     */
    public static boolean isExpiration(String token) {
        return getTokenBody(token).getExpiration().before(new Date());
    }

    private static Claims getTokenBody(String token) {
        return Jwts.parser()
                .setSigningKey(SECRET)
                .parseClaimsJws(token)
                .getBody();
    }
}


