package ru.mssecondteam.taskservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class ApplicationExceptionHandler {

    @ExceptionHandler(DeadlineException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleDeadlineException(DeadlineException ex) {
        Map<String, String> error = Map.of("error", ex.getLocalizedMessage());
        ErrorResponse errorResponse = new ErrorResponse(error, HttpStatus.BAD_REQUEST.value(), LocalDateTime.now());
        log.error(ex.getLocalizedMessage());
        return errorResponse;
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFoundException(NotFoundException ex) {
        Map<String, String> error = Map.of("error", ex.getLocalizedMessage());
        ErrorResponse errorResponse = new ErrorResponse(error, HttpStatus.NOT_FOUND.value(), LocalDateTime.now());
        log.error(ex.getLocalizedMessage());
        return errorResponse;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleAllException(Exception ex) {
        Map<String, String> errors = new HashMap<>();
        errors.put("error", ex.getLocalizedMessage());
        errors.put("stackTrace", getStackTraceAsString(ex));
        ErrorResponse errorResponse = new ErrorResponse(errors, HttpStatus.INTERNAL_SERVER_ERROR.value(), LocalDateTime.now());
        log.error(ex.getLocalizedMessage());
        return errorResponse;
    }

    private String getStackTraceAsString(Exception ex) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        return sw.toString();
    }
}
