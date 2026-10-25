package org.sitenetsoft.quarkus.webfinger.client;

public class WebFingerUnreachableException extends WebFingerException {

    public WebFingerUnreachableException(String message) {
        super(message);
    }

    public WebFingerUnreachableException(String message, Throwable cause) {
        super(message, cause);
    }
}
