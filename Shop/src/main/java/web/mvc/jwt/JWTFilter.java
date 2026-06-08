package web.mvc.jwt;

import com.google.gson.Gson;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import web.mvc.domain.User;
import web.mvc.security.CustomUserDetails;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class JWTFilter extends OncePerRequestFilter {
    private final JWTUtil jwtUtil;
    public JWTFilter(JWTUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        System.out.println("*******************************************");
        String authorization= request.getHeader("Authorization");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            System.out.println("token null");
            filterChain.doFilter(request, response);
            return;
        }

        System.out.println("authorization now");
        String token = authorization.split(" ")[1];

        try {
            if (jwtUtil.isExpired(token)) {
                System.out.println("token expired");
                filterChain.doFilter(request, response);
                return;
            }

            String username = jwtUtil.getUserName(token);
            String id = jwtUtil.getUserId(token);
            String role = jwtUtil.getRole(token);
            Long userNo = jwtUtil.getUserNo(token);

            User user = new User();
            user.setUserNo(userNo);
            user.setUserId(id);
            user.setUserName(username);
            user.setRole(role);

            CustomUserDetails customUserDetails = new CustomUserDetails(user);

            Authentication authToken = new UsernamePasswordAuthenticationToken(
                    customUserDetails,
                    null,
                    customUserDetails.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(authToken);
            System.out.println("authToken = " + authToken);
            filterChain.doFilter(request, response);
        }catch (ExpiredJwtException e){
            SecurityContextHolder.clearContext();
            sendUnauthorized(response, "TOKEN_EXPIRED", "토큰이 만료되었습니다.");
        }catch (JwtException | IllegalArgumentException e){
            SecurityContextHolder.clearContext();
            sendUnauthorized(response, "INVALID_TOKEN", "유효하지 않은 토큰입니다.");
        }
    }

    private void sendUnauthorized(
            HttpServletResponse response,
            String code,
            String detail
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> body = new HashMap<>();
        body.put("status", 401);
        body.put("title", "Unauthorized");
        body.put("detail", detail);
        body.put("code", code);

        response.getWriter().write(new Gson().toJson(body));
    }
}