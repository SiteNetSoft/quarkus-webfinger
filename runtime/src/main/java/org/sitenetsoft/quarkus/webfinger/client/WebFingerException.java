package org.sitenetsoft.quarkus.webfinger.client;

public class WebFingerException extends RuntimeException {

    public WebFingerException(String message) {
        super(message);
    }

    public WebFingerException(String message, Throwable cause) {
        super(message, cause);
    }
}
