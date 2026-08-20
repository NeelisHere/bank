package com.bank.common_lib.exception;


import lombok.Getter;

@Getter
public class CommonException extends RuntimeException {
    private final String status;

    public CommonException(String message, String status) {
        super(message);
        this.status = status;
    }
}
