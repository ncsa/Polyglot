package edu.ncsa.icr.polyglot;

public class PolyglotException extends Exception {
    private static final long serialVersionUID = 1L;

    public PolyglotException(String msg) {
        super(msg);
    }

    public PolyglotException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
