package org.sitenetsoft.quarkus.webfinger.client;

public class WebFingerNotFoundException extends WebFingerException {

    public WebFingerNotFoundException(String message) {
        super(message);
    }

    public WebFingerNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
