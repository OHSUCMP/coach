package edu.ohsu.cmp.coach.exception;

public class HttpException extends Exception {
    private int httpResponseCode;

    public HttpException(int httpResponseCode) {
        super();
        this.httpResponseCode = httpResponseCode;
    }

    public HttpException(int httpResponseCode, String message) {
        super(message);
        this.httpResponseCode = httpResponseCode;
    }

    public HttpException(int httpResponseCode, String message, Throwable cause) {
        super(message, cause);
        this.httpResponseCode = httpResponseCode;
    }

    public int getHttpResponseCode() {
        return httpResponseCode;
    }
}
