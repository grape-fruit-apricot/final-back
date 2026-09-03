package com.kh.midpoint.common.exception;

import com.kh.midpoint.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(NotFoundException.class)
	public ResponseEntity<ApiResponse<Object>> handleNotFoundException(NotFoundException e) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), e.getMessage()));
	}

	@ExceptionHandler(DuplicateException.class)
	public ResponseEntity<ApiResponse<Object>> handleDuplicateException(DuplicateException e) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
			.body(ApiResponse.error(HttpStatus.CONFLICT.value(), e.getMessage()));
	}

	@ExceptionHandler(ForbiddenException.class)
	public ResponseEntity<ApiResponse<Object>> handleForbiddenException(ForbiddenException e) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
			.body(ApiResponse.error(HttpStatus.FORBIDDEN.value(), e.getMessage()));
	}

	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<ApiResponse<Object>> handleUnauthorizedException(UnauthorizedException e) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
			.body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), e.getMessage()));
	}

	@ExceptionHandler(InvalidStateException.class)
	public ResponseEntity<ApiResponse<Object>> handleInvalidStateException(InvalidStateException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
	}

	@ExceptionHandler(ExternalApiException.class)
	public ResponseEntity<ApiResponse<Object>> handleExternalApiException(ExternalApiException e) {
		return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
			.body(ApiResponse.error(HttpStatus.BAD_GATEWAY.value(), e.getMessage()));
	}

	// @Valid 실패. 필드가 여러 개여도 사용자가 먼저 고칠 것은 하나라 첫 메시지만 내려준다.
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Object>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
		FieldError fieldError = e.getBindingResult().getFieldError();
		String message = (fieldError == null) ? "요청 값이 올바르지 않습니다." : fieldError.getDefaultMessage();

		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), message));
	}

	// 본문이 JSON 으로 파싱되지 않는 경우. ErrorResponse 를 구현하지 않아 폴백으로 새는데,
	// 잘못 보낸 요청이므로 500 이 아니라 400 이어야 한다. 파싱 오류 원문은 노출하지 않는다.
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
		log.warn("요청 본문을 읽지 못함: {}", e.getMessage());

		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "요청 형식이 올바르지 않습니다."));
	}

	// UNIQUE 제약 위반이 Service 를 거치지 않고 올라오는 경로(예: 같은 방 닉네임 중복)를 받는다.
	// 이 핸들러가 없으면 Spring 기본 오류 본문으로 500 이 나가 ApiResponse 형태가 깨진다.
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiResponse<Object>> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
		log.warn("데이터 제약 위반", e);

		return ResponseEntity.status(HttpStatus.CONFLICT)
			.body(ApiResponse.error(HttpStatus.CONFLICT.value(), "이미 존재하는 값입니다."));
	}

	// 정적 리소스 404(favicon 등)가 아래 폴백에 걸려 500 으로 둔갑하지 않도록 먼저 잡는다.
	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ApiResponse<Object>> handleNoResourceFoundException(NoResourceFoundException e) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), "존재하지 않는 경로입니다."));
	}

	// 예상하지 못한 예외. 원인 메시지에는 SQL 이나 내부 경로가 섞일 수 있어 응답에는 싣지 않고 로그로만 남긴다.
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Object>> handleException(Exception e) {
		// Spring MVC 표준 예외(405 메서드 불일치, 415 타입 불일치, 본문 파싱 실패 등)는
		// 자기 상태 코드를 갖고 있다. 여기서 뭉뚱그려 500 으로 바꾸면 원인을 구분할 수 없다.
		if (e instanceof ErrorResponse errorResponse) {
			HttpStatusCode statusCode = errorResponse.getStatusCode();

			return ResponseEntity.status(statusCode)
				.body(ApiResponse.error(statusCode.value(), "요청을 처리할 수 없습니다."));
		}

		log.error("처리하지 못한 예외", e);

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "서버 오류가 발생했습니다."));
	}

}
