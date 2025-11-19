package techfix.techfix.common;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ApiError handleValidation(MethodArgumentNotValidException ex) {
		List<String> details = ex.getBindingResult().getFieldErrors().stream()
				.map(fieldError -> "%s: %s".formatted(fieldError.getField(), fieldError.getDefaultMessage()))
				.collect(Collectors.toList());
		return ApiError.badRequest("Falha de validação", details);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ApiError handleConstraintViolation(ConstraintViolationException ex) {
		List<String> details = ex.getConstraintViolations().stream()
				.map(violation -> violation.getPropertyPath() + ": " + violation.getMessage()).toList();
		return ApiError.badRequest("Parâmetros inválidos", details);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiError.of("Recurso não encontrado", List.of(ex.getMessage()), HttpStatus.NOT_FOUND));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleGeneric(Exception ex) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiError.of("Erro interno", List.of(ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR));
	}

	public record ApiError(String message, Instant timestamp, HttpStatus status, List<String> details) {
		static ApiError badRequest(String message, List<String> details) {
			return of(message, details, HttpStatus.BAD_REQUEST);
		}

		static ApiError of(String message, List<String> details, HttpStatus status) {
			return new ApiError(message, Instant.now(), status, details);
		}
	}
}

