package com.ashcollege.responses;

import com.ashcollege.entities.GameEntity;
import com.ashcollege.entities.UserEntity;

import java.util.Date;
import java.util.List;

public class GameModel {
    private String gameName;
    private int gameType;
    private String gameCode;
    private int status;
    private UserModel creator;
    private List<GamePlayerModel> players;
    private Long startedAt;
    private Long finishedAt;
    private int maxPlayers;
    private int trackLength;
    public GameModel(GameEntity game, List<GamePlayerModel> players) {
        this.gameCode = game.getGameCode();
        this.gameName = game.getGameName();
        this.gameType = game.getGameType();
        this.status = game.getStatus();
        this.maxPlayers = game.getMaxPlayers();
        this.trackLength = game.getTrackLength();
        this.startedAt = game.getStartedAt() != null ? game.getStartedAt().getTime() : null;
        this.finishedAt = game.getFinishedAt() != null ? game.getFinishedAt().getTime() : null;
        this.creator = new UserModel(game.getCreator());
        this.players = players;
    }
    public List<GamePlayerModel> getPlayers() {
        return players;
    }

    public void setPlayers(List<GamePlayerModel> players) {
        this.players = players;
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


    public UserModel getCreator() {
        return creator;
    }

    public void setCreator(UserModel creator) {
        this.creator = creator;
    }

    public int getStatus() {
        return status;
    }

    public Long getStartedAt() { return startedAt; }

    public void setStartedAt(Long startedAt) { this.startedAt = startedAt;}

    public Long getFinishedAt() { return finishedAt; }

    public void setFinishedAt(Long finishedAt) { this.finishedAt = finishedAt; }

    public int getMaxPlayers() { return maxPlayers; }

    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }

    public int getTrackLength() { return trackLength; }

    public void setTrackLength(int trackLength) { this.trackLength = trackLength; }

    public void setStatus(int status) { this.status = status; }
}
