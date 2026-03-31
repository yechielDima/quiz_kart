package com.ashcollege.entities;

public class PlayerAnswerEntity {
    private int id;
    private GamePlayerEntity gamePlayer;
    private String questionText;
    private int questionType;
    private int playerAnswer;
    private int correctAnswer;
    private boolean Correct;
    private long timeTakenMs;
    private int pointsEarned;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public GamePlayerEntity getGamePlayer() {
        return gamePlayer;
    }

    public void setGamePlayer(GamePlayerEntity gamePlayer) {
        this.gamePlayer = gamePlayer;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public int getQuestionType() {
        return questionType;
    }

    public void setQuestionType(int questionType) {
        this.questionType = questionType;
    }

    public int getPlayerAnswer() {
        return playerAnswer;
    }

    public void setPlayerAnswer(int playerAnswer) {
        this.playerAnswer = playerAnswer;
    }

    public int getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(int correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public boolean isCorrect() {
        return Correct;
    }

    public void setCorrect(boolean correct) {
        Correct = correct;
    }

    public long getTimeTakenMs() {
        return timeTakenMs;
    }

    public void setTimeTakenMs(long timeTakenMs) {
        this.timeTakenMs = timeTakenMs;
    }

    public int getPointsEarned() {
        return pointsEarned;
    }

    public void setPointsEarned(int pointsEarned) {
        this.pointsEarned = pointsEarned;
    }
}