package com.nomlog.backend.record;

public class RecordNotFoundException extends RuntimeException {

    public RecordNotFoundException(Long id) {
        super("Record not found: " + id);
    }
}
