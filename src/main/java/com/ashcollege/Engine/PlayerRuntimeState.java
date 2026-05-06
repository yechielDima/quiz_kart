package com.ashcollege.Engine;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.ashcollege.utils.Constants.*;

public class PlayerRuntimeState {

    private static final Random random = new Random();

    private int userId;
    private String username;
    private String fullName;
    private int score;
    private int correctAnswers;
    private int wrongAnswers;
    private int streak;
    private int bestStreak;
    private MathQuestionGenerator.QuestionData currentQuestion;
    private int currentCorrectAnswer;
    private final List<QuestionLog> answerHistory = new CopyOnWriteArrayList<>();
    private boolean finished;
    private long currentQuestionStartTime;
    private int currentQuestionDifficulty;

    private int decisionMeter;
    private int decisionThreshold;
    private boolean junctionPending;
    private int junctionType;
    private int dirtRoadQuestionsLeft;

    private int luckMeter;
    private boolean lastLuckEventWasBad;
    private String activeEffect;
    private int luckEventsReceived;

    private int swapsUsed;

    public PlayerRuntimeState() {
        this.decisionThreshold = randomThreshold();
        this.activeEffect = EFFECT_NONE;
    }

    private int randomThreshold() {
        return DECISION_METER_MIN + random.nextInt(DECISION_METER_MAX - DECISION_METER_MIN + 1);
    }

    public void incrementDecisionMeter() {
        decisionMeter++;
    }

    public boolean shouldTriggerJunction() {
        return decisionMeter >= decisionThreshold && junctionType == JUNCTION_NONE && !junctionPending;
    }

    public void triggerJunction() {
        junctionPending = true;
    }

    public void chooseAutostrada() {
        junctionPending = false;
        junctionType = JUNCTION_AUTOSTRADA;
    }

    public void chooseDirtRoad() {
        junctionPending = false;
        junctionType = JUNCTION_DIRT_ROAD;
        dirtRoadQuestionsLeft = DIRT_ROAD_QUESTIONS;
    }

    public void resetJunction() {
        junctionType = JUNCTION_NONE;
        junctionPending = false;
        decisionMeter = 0;
        decisionThreshold = randomThreshold();
        dirtRoadQuestionsLeft = 0;
    }

    public void incrementLuckMeter() {
        int increment = LUCK_METER_INCREMENT_MIN
                + random.nextInt(LUCK_METER_INCREMENT_MAX - LUCK_METER_INCREMENT_MIN + 1);
        luckMeter += increment;
    }

    public boolean shouldTriggerLuckEvent() {
        return luckMeter >= LUCK_METER_THRESHOLD;
    }

    public String rollLuckEvent() {
        luckMeter = 0;
        luckEventsReceived++;

        String[] goodEvents = {LUCK_TURBO, LUCK_DOUBLE_POINTS};
        String[] badEvents = {LUCK_FLAT_TIRE, LUCK_OIL_SLICK};

        if (lastLuckEventWasBad) {
            lastLuckEventWasBad = false;
            return goodEvents[random.nextInt(goodEvents.length)];
        }

        boolean isGood = random.nextInt(100) < 60;

        if (isGood) {
            lastLuckEventWasBad = false;
            return goodEvents[random.nextInt(goodEvents.length)];
        } else {
            lastLuckEventWasBad = true;
            return badEvents[random.nextInt(badEvents.length)];
        }
    }

    public void applyLuckEvent(String event) {
        switch (event) {
            case LUCK_TURBO:
                score += TURBO_BONUS;
                break;
            case LUCK_OIL_SLICK:
                score = Math.max(0, score - OIL_SLICK_PENALTY);
                break;
            case LUCK_DOUBLE_POINTS:
                activeEffect = EFFECT_DOUBLE_POINTS;
                break;
            case LUCK_FLAT_TIRE:
                activeEffect = EFFECT_FLAT_TIRE;
                break;
        }
    }

    public int applyActiveEffect(int basePoints) {
        if (EFFECT_DOUBLE_POINTS.equals(activeEffect)) {
            activeEffect = EFFECT_NONE;
            return basePoints * 2;
        }
        if (EFFECT_FLAT_TIRE.equals(activeEffect)) {
            activeEffect = EFFECT_NONE;
            return 0;
        }
        return basePoints;
    }

    public double getAverageAnswerTimeMs() {
        if (answerHistory.isEmpty()) return 0;
        long total = 0;
        for (QuestionLog log : answerHistory) {
            total += log.getTimeTakenMs();
        }
        return (double) total / answerHistory.size();
    }

    public long getCurrentQuestionStartTime() {
        return currentQuestionStartTime;
    }

    public void setCurrentQuestionStartTime(long currentQuestionStartTime) {
        this.currentQuestionStartTime = currentQuestionStartTime;
    }

    public int getCurrentCorrectAnswer() {
        return currentCorrectAnswer;
    }

    public void setCurrentCorrectAnswer(int currentCorrectAnswer) {
        this.currentCorrectAnswer = currentCorrectAnswer;
    }

    public int getCurrentQuestionDifficulty() {
        return currentQuestionDifficulty;
    }

    public void setCurrentQuestionDifficulty(int currentQuestionDifficulty) {
        this.currentQuestionDifficulty = currentQuestionDifficulty;
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
        if (streak > bestStreak) {
            bestStreak = streak;
        }
    }

    public int getBestStreak() {
        return bestStreak;
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

    public int getDecisionMeter() {
        return decisionMeter;
    }

    public boolean isJunctionPending() {
        return junctionPending;
    }

    public int getJunctionType() {
        return junctionType;
    }

    public int getDirtRoadQuestionsLeft() {
        return dirtRoadQuestionsLeft;
    }

    public void setDirtRoadQuestionsLeft(int dirtRoadQuestionsLeft) {
        this.dirtRoadQuestionsLeft = dirtRoadQuestionsLeft;
    }

    public int getLuckMeter() {
        return luckMeter;
    }

    public String getActiveEffect() {
        return activeEffect;
    }

    public void setActiveEffect(String activeEffect) {
        this.activeEffect = activeEffect;
    }

    public int getLuckEventsReceived() {
        return luckEventsReceived;
    }

    public int getSwapsUsed() {
        return swapsUsed;
    }

    public void incrementSwapsUsed() {
        swapsUsed++;
    }
}