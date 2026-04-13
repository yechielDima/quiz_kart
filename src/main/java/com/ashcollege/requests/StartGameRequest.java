package com.ashcollege.requests;

public class StartGameRequest {
    private String token;
    private int gameId;

    public StartGameRequest() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getGameId() {
        return gameId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }
}