package com.ezielnik.api.user;

public class AdminWarningRequest {

    private String subject;
    private String message;

    public AdminWarningRequest() {
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}