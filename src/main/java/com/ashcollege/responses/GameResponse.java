package com.ashcollege.responses;

import com.ashcollege.entities.GameEntity;
import com.ashcollege.entities.UserEntity;

import java.util.List;

public class GameResponse extends BasicResponse{
    private GameModel gameModel;
    public GameResponse() {}

    public GameResponse(boolean success, Integer errorCode, GameEntity gameEntity, List<UserEntity> players) {
        super(success, errorCode);
        this.gameModel = new GameModel(gameEntity, players);
    }

    public GameModel getGameModel() {
        return gameModel;
    }

    public void setGameModel(GameModel gameModel) {
        this.gameModel = gameModel;
    }
}
