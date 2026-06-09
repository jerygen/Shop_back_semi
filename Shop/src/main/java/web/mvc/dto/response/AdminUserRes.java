package web.mvc.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import web.mvc.domain.User;

import java.sql.Timestamp;

@Getter
@AllArgsConstructor
public class AdminUserRes {
    private Long userNo;
    private String userId;
    private String userName;
    private String role;
    private Timestamp regDate;
    private Long orderCount;

    public AdminUserRes(User user, Long orderCount) {
        this.userNo = user.getUserNo();
        this.userId = user.getUserId();
        this.userName = user.getUserName();
        this.role = user.getRole();
        this.regDate = user.getRegDate();
        this.orderCount = orderCount;
    }
}
