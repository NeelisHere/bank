package com.bank.common_lib.exception;

import java.time.LocalDateTime;

public record ErrorResponse(
        String message,
        String status,
        LocalDateTime timestamp
) {
}
