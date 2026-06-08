package web.mvc.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import web.mvc.domain.User;
import web.mvc.dto.response.ApiResponse;
import web.mvc.service.UserService;

@RestController
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final UserService userService;

    @RequestMapping("/register")
    public ResponseEntity<ApiResponse<User>> register(User user) {
        userService.signUp(user);
        return ResponseEntity.ok(ApiResponse.created(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        return ResponseEntity.ok(ApiResponse.success("로그아웃 되었습니다."));
    }
}
