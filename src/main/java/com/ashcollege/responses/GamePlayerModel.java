package com.ashcollege.responses;

import com.ashcollege.Engine.PlayerRuntimeState;
import com.ashcollege.entities.GamePlayerEntity;

import java.util.Date;

public class GamePlayerModel {
    private int id;
    private String fullName;
    private int score;
    private int correctAnswers;
    private int streak;
    private int wrongAnswers;
    private boolean finished;
    private Date finishTime;

    public GamePlayerModel(GamePlayerEntity gp) {
        this.id = gp.getPlayer().getId();
        this.fullName = gp.getPlayer().getFullName();
        this.score = gp.getScore();
        this.correctAnswers = gp.getCorrectAnswers();
        this.streak = gp.getStreak();
        this.wrongAnswers = gp.getWrongAnswers();
        this.finished = gp.isFinished();
        this.finishTime = gp.getFinishTime();
    }

    public GamePlayerModel(PlayerRuntimeState prs) {
        this.id = prs.getUserId();
        this.fullName = prs.getFullName();
        this.score = prs.getScore();
        this.correctAnswers = prs.getCorrectAnswers();
        this.streak = prs.getStreak();
        this.wrongAnswers = prs.getWrongAnswers();
        this.finished = prs.isFinished();

    }

    public int getWrongAnswers() {
        return wrongAnswers;
    }

    public void setWrongAnswers(int wrongAnswers) {
        this.wrongAnswers = wrongAnswers;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public Date getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Date finishTime) {
        this.finishTime = finishTime;
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