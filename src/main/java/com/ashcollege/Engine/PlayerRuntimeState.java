package com.ashcollege.Engine;

import java.util.ArrayList;
import java.util.List;

public class PlayerRuntimeState {

    private int userId;
    private String username;
    private String fullName;

    private int score;

    private int correctAnswers;
    private int wrongAnswers;
    private int streak;
    // השאלה שהרגע הוגרלה לשחקן והוא עדיין לא ענה עליה
    private MathQuestionGenerator.QuestionData currentQuestion;
    // בתוך PlayerRuntimeState.java להוסיף:
    private int currentCorrectAnswer;

    private final List<QuestionLog> answerHistory = new ArrayList<>();
    private boolean finished;
    // --- התוספת שחסרה למדידת הזמנים ---
    private long currentQuestionStartTime;

    public long getCurrentQuestionStartTime() {
        return currentQuestionStartTime;
    }

    public void setCurrentQuestionStartTime(long currentQuestionStartTime) {
        this.currentQuestionStartTime = currentQuestionStartTime;
    }
    // ---------------------------------

    public int getCurrentCorrectAnswer() {
        return currentCorrectAnswer;
    }

    public void setCurrentCorrectAnswer(int currentCorrectAnswer) {
        this.currentCorrectAnswer = currentCorrectAnswer;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getCorrectAnswers() {
        return correctAnswers;
    }

    public void setCorrectAnswers(int correctAnswers) {
        this.correctAnswers = correctAnswers;
    }

    public int getWrongAnswers() {
        return wrongAnswers;
    }

    public void setWrongAnswers(int wrongAnswers) {
        this.wrongAnswers = wrongAnswers;
    }

    public int getStreak() {
        return streak;
    }

    public void setStreak(int streak) {
        this.streak = streak;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }
    public MathQuestionGenerator.QuestionData getCurrentQuestion() {
        return currentQuestion;
    }

    public void setCurrentQuestion(MathQuestionGenerator.QuestionData currentQuestion) {
        this.currentQuestion = currentQuestion;
    }

    public List<QuestionLog> getAnswerHistory() {
        return answerHistory;
    }
}
