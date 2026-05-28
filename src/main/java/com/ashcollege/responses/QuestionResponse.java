package com.ashcollege.responses;

import java.util.List;

public class QuestionResponse extends BasicResponse {
    private String questionText;
    private List<Integer> options;
    private int timeLimitSeconds;
    private String questionMode;
    private int dirtRoadRemaining;

    public QuestionResponse(boolean success, Integer errorCode, String questionText,
                            List<Integer> options, int timeLimitSeconds,
                            String questionMode, int dirtRoadRemaining) {
        super(success, errorCode);
        this.questionText = questionText;
        this.options = options;
        this.timeLimitSeconds = timeLimitSeconds;
        this.questionMode = questionMode;
        this.dirtRoadRemaining = dirtRoadRemaining;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public List<Integer> getOptions() {
        return options;
    }

    public void setOptions(List<Integer> options) {
        this.options = options;
    }

    public int getTimeLimitSeconds() {
        return timeLimitSeconds;
    }

    public void setTimeLimitSeconds(int timeLimitSeconds) {
        this.timeLimitSeconds = timeLimitSeconds;
    }

    public String getQuestionMode() {
        return questionMode;
    }

    public void setQuestionMode(String questionMode) {
        this.questionMode = questionMode;
    }

    public int getDirtRoadRemaining() {
        return dirtRoadRemaining;
    }

    public void setDirtRoadRemaining(int dirtRoadRemaining) {
        this.dirtRoadRemaining = dirtRoadRemaining;
    }
}