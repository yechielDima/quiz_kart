package com.ashcollege.responses;

public class LoginResponse extends BasicResponse {
    private int permission;
    private String token;
    private int id;


    public LoginResponse(boolean success, Integer errorCode, int permission, String token, int id) {
        super(success, errorCode);
        this.permission = permission;
        this.token = token;
        this.id = id;

    }

    public LoginResponse(int permission, String token, int id) {
        this.permission = permission;
        this.token = token;
        this.id = id;
    }

    public int getPermission() {
        return permission;
    }

    public void setPermission(int permission) {
        this.permission = permission;
    }

    public LoginResponse() {

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }


}
