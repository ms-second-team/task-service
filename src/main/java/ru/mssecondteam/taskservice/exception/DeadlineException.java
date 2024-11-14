package ru.mssecondteam.taskservice.exception;

public class DeadlineException extends RuntimeException {

    public DeadlineException(String message) {
        super(message);
    }
}
