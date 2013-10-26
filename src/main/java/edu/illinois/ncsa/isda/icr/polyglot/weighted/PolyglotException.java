package edu.illinois.ncsa.isda.icr.polyglot.weighted;

public class PolyglotException extends Exception {
    private static final long serialVersionUID = 1L;

    public PolyglotException(String msg) {
        super(msg);
    }

    public PolyglotException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
