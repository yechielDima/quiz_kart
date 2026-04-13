package com.ashcollege.requests;

public class JoinGameRequest {
    private String token;
    private String gameCode;

    public JoinGameRequest() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getGameCode() {
        return gameCode;
    }

    public void setGameCode(String gameCode) {
        this.gameCode = gameCode;
    }
}