package ng.fixpay.shared.exception;

public class FixPayException extends RuntimeException {

    private final String errorCode;
    private final int    httpStatus;

    public FixPayException(String message, String errorCode, int httpStatus) {
        super(message);
        this.errorCode  = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode()  { return errorCode;  }
    public int    getHttpStatus() { return httpStatus; }

    public static FixPayException notFound(String resource) {
        return new FixPayException(resource + " not found", "NOT_FOUND", 404);
    }

    public static FixPayException forbidden(String reason) {
        return new FixPayException(reason, "FORBIDDEN", 403);
    }

    public static FixPayException badRequest(String reason) {
        return new FixPayException(reason, "BAD_REQUEST", 400);
    }

    public static FixPayException conflict(String reason) {
        return new FixPayException(reason, "CONFLICT", 409);
    }

    public static FixPayException unauthorized(String reason) {
        return new FixPayException(reason, "UNAUTHORIZED", 401);
    }
}
