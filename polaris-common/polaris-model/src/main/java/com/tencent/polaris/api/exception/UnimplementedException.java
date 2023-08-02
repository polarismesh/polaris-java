package com.tencent.polaris.api.exception;

public class UnimplementedException extends PolarisException {
    public UnimplementedException() {
        super(ErrorCode.INTERNAL_ERROR);
    }

    public UnimplementedException(String message) {
        super(ErrorCode.INTERNAL_ERROR, message);
    }

    public UnimplementedException(String message, Throwable cause) {
        super(ErrorCode.INTERNAL_ERROR, message, cause);
    }
}
