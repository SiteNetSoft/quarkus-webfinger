package org.sitenetsoft.quarkus.webfinger.client;

public class WebFingerInvalidResponseException extends WebFingerException {

    public WebFingerInvalidResponseException(String message) {
        super(message);
    }

    public WebFingerInvalidResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
