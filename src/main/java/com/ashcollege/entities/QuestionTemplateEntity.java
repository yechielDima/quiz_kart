package com.ashcollege.entities;

public class QuestionTemplateEntity extends BaseEntity {

    private String templateText; // הטקסט, לדוגמה: "ל{name1} היו {num1} {obj}. הוא נתן {num2} ל{name2}. כמה נשארו לו?"
    private int operationType;   // 0 = חיבור, 1 = חיסור, 2 = כפל, 3 = חילוק, 4 = אחוזים
    private int difficultyLevel; // 0 = קל, 1 = בינוני, 2 = קשה

    public String getTemplateText() {
        return templateText;
    }

    public void setTemplateText(String templateText) {
        this.templateText = templateText;
    }

    public int getOperationType() {
        return operationType;
    }

    public void setOperationType(int operationType) {
        this.operationType = operationType;
    }

    public int getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(int difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }
}