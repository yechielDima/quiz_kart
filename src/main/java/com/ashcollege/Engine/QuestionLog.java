package com.ashcollege.Engine;

public class QuestionLog {
    private String questionText;
    private int questionType;
    private int playerAnswer;
    private int correctAnswer;
    private boolean isCorrect;

    // השדות החדשים
    private long timeTakenMs;
    private int pointsEarned;

    // בנאי מעודכן
    public QuestionLog(String questionText, int questionType, int playerAnswer,
                       int correctAnswer, boolean isCorrect, long timeTakenMs, int pointsEarned) {
        this.questionText = questionText;
        this.questionType = questionType;
        this.playerAnswer = playerAnswer;
        this.correctAnswer = correctAnswer;
        this.isCorrect = isCorrect;
        this.timeTakenMs = timeTakenMs;
        this.pointsEarned = pointsEarned;
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
        return isCorrect;
    }

    public void setCorrect(boolean correct) {
        isCorrect = correct;
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