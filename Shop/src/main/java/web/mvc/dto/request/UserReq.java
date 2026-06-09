package web.mvc.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import web.mvc.domain.User;

@Getter
@Setter
@ToString
public class UserReq {
    private String userName;
    private String password;

    public User toUser(UserReq userReq){
        return User.builder()
                .userName(userReq.getUserName())
                .password(userReq.getPassword())
                .build();
    }
}
