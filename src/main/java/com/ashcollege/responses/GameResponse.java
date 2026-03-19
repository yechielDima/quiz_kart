package com.ashcollege.responses;

import com.ashcollege.entities.GameEntity;
import java.util.List;

public class GameResponse extends BasicResponse {
    private GameModel gameModel;

    public GameResponse() {}

    public GameResponse(boolean success, Integer errorCode, GameEntity gameEntity, List<GamePlayerModel> players) {
        super(success, errorCode);
        this.gameModel = new GameModel(gameEntity, players);
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