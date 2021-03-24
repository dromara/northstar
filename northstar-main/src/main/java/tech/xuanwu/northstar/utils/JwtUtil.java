package tech.xuanwu.northstar.utils;

import java.util.Date;
import java.util.HashMap;

import org.apache.commons.lang3.RandomStringUtils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;

public class JwtUtil {
	/**
	 * 4小时有效期
	 */
	private static final long EXP_DURATION = 4*3600*1000;

    /**
     * token私钥
     */
    private static final String TOKEN_SECRET = RandomStringUtils.random(20);

    /**
     * 生成签名
     * @param username
     * @param userId
     * @return
     */
    public static String sign(String username, String password){
        //私钥及加密算法
        Algorithm algorithm = Algorithm.HMAC256(TOKEN_SECRET);
        //设置头信息
        HashMap<String, Object> header = new HashMap<>(2);
        header.put("typ", "JWT");
        header.put("alg", "HS256");
        //附带username和userID生成签名
        return JWT.create().withHeader(header).withClaim("username",username)
                .withClaim("password",password).withExpiresAt(new Date(System.currentTimeMillis() + EXP_DURATION)).sign(algorithm);
    }


    public static boolean verity(String token){
        try {
            Algorithm algorithm = Algorithm.HMAC256(TOKEN_SECRET);
            JWTVerifier verifier = JWT.require(algorithm).build();
            verifier.verify(token);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        } catch (JWTVerificationException e) {
            return false;
        }

    }
}
