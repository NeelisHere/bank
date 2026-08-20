package com.bank.common_lib.dto.response;


import java.time.LocalDateTime;

public record GenericResponse(String message, String status, LocalDateTime timestamp) { }
