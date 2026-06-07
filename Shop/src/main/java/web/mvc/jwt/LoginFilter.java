package web.mvc.jwt;

import com.google.gson.Gson;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import web.mvc.domain.User;
import web.mvc.security.CustomUserDetails;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Slf4j
public class LoginFilter extends UsernamePasswordAuthenticationFilter {
    private final AuthenticationManager authenticationManager;
    private final JWTUtil jwtUtil;

    public LoginFilter(AuthenticationManager authenticationManager, JWTUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        String username = super.obtainUsername(request);
        String password = super.obtainPassword(request);

        log.info("username={}", username);
        log.info("password={}", password);

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, password);

        Authentication authentication = authenticationManager.authenticate(authToken);
        log.info("authentication = {}", authentication);
        return authentication;
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain,
                                            Authentication authentication) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        log.info("login success");

        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
        GrantedAuthority auth = iterator.next();
        String role = auth.getAuthority();

        String token = jwtUtil.createJwt(customUserDetails.getUser(), role, 1000L * 60 * 10L);
        response.addHeader("Authorization", "Bearer " + token);

        User user = customUserDetails.getUser();
        Map<String, Object> map = new HashMap<>();
        map.put("userNo", user.getUserNo());
        map.put("userId", user.getUserId());
        map.put("userName", user.getUserName());
        map.put("role", user.getRole());

        Gson gson = new Gson();
        response.getWriter().print(gson.toJson(map));
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request,
                                              HttpServletResponse response,
                                              AuthenticationException failed) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        log.info("login failed");
        log.error("failed={}", failed);

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        Map<String, Object> map = new HashMap<>();
        map.put("errMsg", "Invalid username or password.");

        Gson gson = new Gson();
        response.getWriter().print(gson.toJson(map));
    }
}
