package tech.xuanwu.northstar.utils;

import java.util.Date;
import java.util.HashMap;

import org.apache.commons.lang3.RandomStringUtils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

public class JwtUtil {

	/**
     * 过期时间为一天
     * TODO 正式上线更换为15分钟
     */
    private static final long EXPIRE_TIME = 120*1000;

    /**
     * token私钥
     */
    private static final String TOKEN_SECRET = RandomStringUtils.random(20);

    /**
     * 生成签名,15分钟后过期
     * @param username
     * @param userId
     * @return
     */
    public static String sign(String username, String password){
        //过期时间
        Date date = new Date(System.currentTimeMillis() + EXPIRE_TIME);
        //私钥及加密算法
        Algorithm algorithm = Algorithm.HMAC256(TOKEN_SECRET);
        //设置头信息
        HashMap<String, Object> header = new HashMap<>(2);
        header.put("typ", "JWT");
        header.put("alg", "HS256");
        //附带username和userID生成签名
        return JWT.create().withHeader(header).withClaim("username",username)
                .withClaim("password",password).withExpiresAt(date).sign(algorithm);
    }


    public static boolean verity(String token){
        try {
            Algorithm algorithm = Algorithm.HMAC256(TOKEN_SECRET);
            JWTVerifier verifier = JWT.require(algorithm).build();
            DecodedJWT jwt = verifier.verify(token);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        } catch (JWTVerificationException e) {
            return false;
        }

    }
}
