package com.ashcollege.responses;

import com.ashcollege.entities.GameEntity;
import com.ashcollege.entities.UserEntity;

import java.util.List;

public class GameModel {
    private String gameName;
    private int gameType;
    private String gameCode;
    private int status;
    private UserModel creator;
    private List<UserModel> players;
    public GameModel(GameEntity game,List<UserEntity> players) {
        this.gameCode = game.getGameCode();
        this.gameName = game.getGameName();
        this.gameType = game.getGameType();
        this.status = game.getStatus();
        this.creator = new UserModel(game.getCreator());
        if (players != null) {
            this.players = players.stream().map(UserModel::new).toList();
        }
    }

    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public int getGameType() {
        return gameType;
    }

    public void setGameType(int gameType) {
        this.gameType = gameType;
    }

    public String getGameCode() {
        return gameCode;
    }

    public void setGameCode(String gameCode) {
        this.gameCode = gameCode;
    }

    public List<UserModel> getPlayers() {
        return players;
    }

    public void setPlayers(List<UserModel> players) {
        this.players = players;
    }

    public UserModel getCreator() {
        return creator;
    }

    public void setCreator(UserModel creator) {
        this.creator = creator;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
