package edu.ohsu.cmp.coach.exception;

import java.io.IOException;

public class MyHttpException extends IOException {
    private int httpResponseCode;

    public MyHttpException(int httpResponseCode) {
        super();
        this.httpResponseCode = httpResponseCode;
    }

    public MyHttpException(int httpResponseCode, String message) {
        super(message);
        this.httpResponseCode = httpResponseCode;
    }

    public MyHttpException(int httpResponseCode, String message, Throwable cause) {
        super(message, cause);
        this.httpResponseCode = httpResponseCode;
    }

    public int getHttpResponseCode() {
        return httpResponseCode;
    }
}
