package web.mvc.jwt;

import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import web.mvc.domain.User;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/*
 JWT 정보 검증및 생성
 */
@Component //생성
@Slf4j
public class JWTUtil {

    private SecretKey secretKey;//Decode한 secret key를 담는 객체
    
    //application.properties에 있는 미리 Base64로 Encode된 Secret key를 가져온다
    public JWTUtil(@Value("${spring.jwt.secret}")String secret) {
        secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), Jwts.SIG.HS256.key().build().getAlgorithm());
    }

    /////////나중에 토큰을 검증할 때 사용하기 위한 getXxx //////////////////////
    //검증 Username
    public String getUserName(String token) {
         log.info("getUsername(String token)  call");
        String re = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("userName", String.class);
        log.info("getUsername(String token)  re = {}" ,re);
        return re;
    }
    //검증 Id
    public String getUserId(String token) {
        log.info("getId(String token)  call");
        String re = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("userId", String.class);
        log.info("getIds(String token)  re = {}" ,re);
        return re;
    }
    //검증 memberNo
    public Long getUserNo(String token) {
        log.info("getMemberNo(String token)  call");
        Long re = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("userNo", Long.class);
        log.info("getMemberNo(String token)  re = {}" ,re);
        return re;
    }
    //검증 Role
    public String getRole(String token) {
        log.info("getRole(String token)  call");
        String re = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("role", String.class);
        log.info("getRole(String token)  re = {} " , re);
        return re;
    }
    //검증 Expired
    public Boolean isExpired(String token) {
        log.info("isExpired(String token)  call");
        boolean re = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().getExpiration().before(new Date());
        log.info("isExpired(String token)  re  = {}",re);
        return re;
    }

    public String createJwt(User user, String role, Long expiredMs) {
        log.info("createJwt  call");
        return Jwts.builder()
                .claim("userNo", user.getUserNo())
                .claim("userName", user.getUserName())
                .claim("userId", user.getUserId())
                .claim("role", role)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiredMs))
                .signWith(secretKey)
                .compact();

    }
}



