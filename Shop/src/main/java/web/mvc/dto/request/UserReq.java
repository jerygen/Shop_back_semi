package web.mvc.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import web.mvc.domain.User;

@Getter
@Setter
@ToString
public class UserReq {
    private String userId;
    private String password;
    private String userName;

    public User toUser() {
        return User.builder()
                .userId(userId)
                .password(password)
                .userName(userName)
                .build();
    }
}
