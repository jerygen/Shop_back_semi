package web.mvc.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	DUPLICATED(HttpStatus.BAD_REQUEST , "Duplicate Id", " 아이디가 중복입니다."),
	WRONG_PASS( HttpStatus.BAD_REQUEST, "password wrong","비밀번호 오류입니다.."),

	NOTFOUND_NO(HttpStatus.NOT_FOUND, "Not Found Board SearchById","글번호를 확인하세요."),

    UPDATE_FAILED( HttpStatus.BAD_REQUEST, "Update fail","수정할 수 없습니다."),
    DELETE_FAILED( HttpStatus.BAD_REQUEST, "Delete fail","삭제할 수 없습니다."),
    INSERT_FAILED( HttpStatus.BAD_REQUEST, "Insert fail","등록할 수 없습니다.");

    private final HttpStatus httpStatus;
    private  final String title;
    private final String message;
}



