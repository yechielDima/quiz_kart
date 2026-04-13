package com.ashcollege.responses;

public class NewGameResponse extends BasicResponse {
    private int gameId;

    public NewGameResponse() {
    }

    public NewGameResponse(boolean success, Integer errorCode, int gameId) {
        super(success, errorCode);
        this.gameId = gameId;
    }

    public int getGameId() {
        return gameId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }
}