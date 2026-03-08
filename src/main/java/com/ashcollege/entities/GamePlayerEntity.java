package com.ashcollege.entities;

public class GamePlayerEntity extends BaseEntity{
    private UserEntity player;
    private GameEntity game;
    private int score;



    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public GameEntity getGame() {
        return game;
    }

    public void setGame(GameEntity game) {
        this.game = game;
    }

    public UserEntity getPlayer() {
        return player;
    }

    public void setPlayer(UserEntity player) {
        this.player = player;
    }
}
