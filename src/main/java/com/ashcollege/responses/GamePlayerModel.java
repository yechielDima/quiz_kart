package com.ashcollege.responses;

import com.ashcollege.Engine.PlayerRuntimeState;
import com.ashcollege.entities.GamePlayerEntity;

public class GamePlayerModel {
    private int id;
    private String fullName;
    private int score;
    private int correctAnswers;
    private int streak;

    public GamePlayerModel(GamePlayerEntity gp) {
        this.id = gp.getPlayer().getId();
        this.fullName = gp.getPlayer().getFullName();
        this.score = gp.getScore();
        this.correctAnswers = gp.getCorrectAnswers();
        this.streak = gp.getStreak();
    }

    public GamePlayerModel(PlayerRuntimeState prs) {
        this.id = prs.getUserId();
        this.fullName = prs.getFullName();
        this.score = prs.getScore();
        this.correctAnswers = prs.getCorrectAnswers();
        this.streak = prs.getStreak();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int getCorrectAnswers() { return correctAnswers; }
    public void setCorrectAnswers(int correctAnswers) { this.correctAnswers = correctAnswers; }
    public int getStreak() { return streak; }
    public void setStreak(int streak) { this.streak = streak; }
}