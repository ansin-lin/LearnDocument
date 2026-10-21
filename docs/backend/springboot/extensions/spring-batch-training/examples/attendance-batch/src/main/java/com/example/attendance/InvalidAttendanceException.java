package com.example.attendance;

public class InvalidAttendanceException extends RuntimeException {
    public InvalidAttendanceException(long recordId, String reason) {
        super("recordId=" + recordId + ": " + reason);
    }
}
