package com.ns.user.exception;

public class AuthException extends RuntimeException{
    private final ExceptionStatus status;
    /**
     * @param status exception에 대한 정보에 대한 enum
     */
    public AuthException(ExceptionStatus status) {
        this.status = status;
    }

    public ExceptionStatus getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return status.getMessage();
    }
}
