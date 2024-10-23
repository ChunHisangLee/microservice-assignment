package com.jack.common.constants;

public enum EventStatus {
    PENDING, // Event is waiting to be processed
    PROCESSING, // Event is currently being processed
    PROCESSED, // Event has been successfully processed
    FAILED // Event failed to process
}
