package errors;

public class SkyflowException extends Exception {
    private ErrorCodesEnum code;

    public SkyflowException(ErrorCodesEnum code, String message) {
        super(message);
        this.setCode(code);
    }

    public SkyflowException(ErrorCodesEnum code, String message, Throwable cause) {
        super(message, cause);
        this.setCode(code);
    }

    public ErrorCodesEnum getCode() {
        return code;
    }

     void setCode(ErrorCodesEnum code) {
        this.code = code;
    }
}
