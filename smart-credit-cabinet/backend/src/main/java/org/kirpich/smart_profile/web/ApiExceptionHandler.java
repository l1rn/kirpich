package org.kirpich.smart_profile.web;

import java.util.List;

import org.kirpich.smart_profile.domain.TransitionException;
import org.kirpich.smart_profile.service.ForbiddenActionException;
import org.kirpich.smart_profile.service.NotFoundException;
import org.kirpich.smart_profile.service.ValidationFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Ошибки в формате RFC 9457 (Problem Details). Поле errors — список сообщений, которые можно показать пользователю. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Не найдено", List.of(e.getMessage()));
    }

    @ExceptionHandler(ValidationFailedException.class)
    ProblemDetail validation(ValidationFailedException e) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "Проверьте данные", e.errors());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalidBody(MethodArgumentNotValidException e) {
        List<String> errors = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .toList();
        return problem(HttpStatus.BAD_REQUEST, "Некорректный запрос", errors);
    }

    @ExceptionHandler(ForbiddenActionException.class)
    ProblemDetail forbidden(ForbiddenActionException e) {
        return problem(HttpStatus.FORBIDDEN, "Недостаточно прав", List.of(e.getMessage()));
    }

    @ExceptionHandler(TransitionException.class)
    ProblemDetail transition(TransitionException e) {
        return problem(HttpStatus.CONFLICT, "Действие недоступно в текущем статусе", List.of(e.getMessage()));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail staleData(ObjectOptimisticLockingFailureException e) {
        return problem(HttpStatus.CONFLICT, "Заявку уже изменили",
                List.of("Заявку только что изменил кто-то другой — обновите страницу"));
    }

    private static ProblemDetail problem(HttpStatus status, String title, List<String> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, String.join("; ", errors));
        problem.setTitle(title);
        problem.setProperty("errors", errors);
        return problem;
    }
}
