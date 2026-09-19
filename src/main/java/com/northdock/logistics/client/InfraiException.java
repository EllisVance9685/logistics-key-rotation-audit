package com.northdock.logistics.client;

public final class InfraiException extends RuntimeException {
    private final int status;
    private final String code;

    public InfraiException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public int status() { return status; }
    public String code() { return code; }
}
