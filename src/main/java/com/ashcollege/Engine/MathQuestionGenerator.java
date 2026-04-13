package com.ashcollege.Engine;

import java.util.List;

public class MathQuestionGenerator {

    // השארנו רק את המודל שמחזיק את הנתונים, הלוגיקה עברה ל-Service!
    public static class QuestionData {
        public String questionText;
        public int correctAnswer;
        public List<Integer> options;
    }
}