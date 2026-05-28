package com.ashcollege.Engine;

public class QuestionLog {
    private String questionText;
    private int questionType;
    private int playerAnswer;
    private int correctAnswer;
    private boolean correct;
    private long timeTakenMs;
    private int pointsEarned;

    public QuestionLog(String questionText, int questionType, int playerAnswer,
                       int correctAnswer, boolean correct, long timeTakenMs, int pointsEarned) {
        this.questionText = questionText;
        this.questionType = questionType;
        this.playerAnswer = playerAnswer;
        this.correctAnswer = correctAnswer;
        this.correct = correct;
        this.timeTakenMs = timeTakenMs;
        this.pointsEarned = pointsEarned;
    }

    public String getQuestionText() { return questionText; }
    public int getQuestionType() { return questionType; }
    public int getPlayerAnswer() { return playerAnswer; }
    public int getCorrectAnswer() { return correctAnswer; }
    public boolean isCorrect() { return correct; }
    public long getTimeTakenMs() { return timeTakenMs; }
    public int getPointsEarned() { return pointsEarned; }
}