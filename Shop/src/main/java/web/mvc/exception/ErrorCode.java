package web.mvc.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND , "Product Not Found", "상품을 찾을 수 없습니다."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "Order Not Found","주문을 찾을 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User Not Found", "사용자를 찾을 수 없습니다."),
    CHAT_NOT_FOUND(HttpStatus.NOT_FOUND, "Chat Not Found", "채팅창을 찾을 수 없습니다."),

    CART_EMPTY(HttpStatus.BAD_REQUEST, "Cart Empty", "장바구니가 비어 있습니다."),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "Insufficient Stock", "주문수량보다 재고가 적습니다."),
    DUPLICATE_PRODUCT_ID(HttpStatus.CONFLICT, "Duplicate Product Id", "상품 아이디가 중복입니다."),

    DUPLICATE_USER_ID(HttpStatus.CONFLICT, "Duplicate User Id", "사용 중인 유저 아이디입니다."),

    NOT_ORDER_OWNER(HttpStatus.FORBIDDEN, "Not Order Owner", "고개님의 주문이 아닙니다."),
    NOT_CHAT_ROOM_PARTICIPANT(HttpStatus.FORBIDDEN, "Not Chat Room Participant", "채팅방 사용자가 아닙니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "Invalid Input", "잘못된 입력입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "서버 내부 오류");

    private final HttpStatus httpStatus;
    private  final String title;
    private final String message;
}



