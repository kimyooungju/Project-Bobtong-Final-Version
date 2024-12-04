package com.example.project_bobtong;

public class User {
    private String email;
    private String handle;

    public User() {
    }

    public User(String email, String handle) {
        this.email = email;
        this.handle = handle;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }
}
