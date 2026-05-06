package com.ashcollege.requests;

public class GameActionRequest {
    private String token;
    private int gameId;
    private Integer answer;
    private Integer choice;

    public GameActionRequest() {
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

    public Integer getAnswer() {
        return answer;
    }

    public void setAnswer(Integer answer) {
        this.answer = answer;
    }

    public Integer getChoice() {
        return choice;
    }

    public void setChoice(Integer choice) {
        this.choice = choice;
    }
}