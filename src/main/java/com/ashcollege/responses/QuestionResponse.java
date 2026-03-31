package com.ashcollege.responses;

import java.util.List;

public class QuestionResponse extends BasicResponse {
    private String questionText;
    private List<Integer> options;

    public QuestionResponse(boolean success, Integer errorCode, String questionText, List<Integer> options) {
        super(success, errorCode);
        this.questionText = questionText;
        this.options = options;
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
}