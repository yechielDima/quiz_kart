package com.ashcollege.responses;

public class GameResponse extends BasicResponse {
    private GameModel gameModel;

    public GameResponse() {
    }

    public GameResponse(boolean success, Integer errorCode, GameModel gameModel) {
        super(success, errorCode);
        this.gameModel = gameModel;
    }

    public GameModel getGameModel() {
        return gameModel;
    }

    public void setGameModel(GameModel gameModel) {
        this.gameModel = gameModel;
    }
}