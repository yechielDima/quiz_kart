package com.ashcollege.Engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MathQuestionGenerator {
    private static final Random random = new Random();

    // מחלקה פנימית ששומרת את כל הנתונים של השאלה
    public static class QuestionData {
        public String questionText;
        public int correctAnswer;
        public List<Integer> options;
    }

    public static QuestionData generateQuestion() {
        QuestionData q = new QuestionData();

        // הגרלת שני מספרים (אפשר בהמשך לקחת רמת קושי מה-GameEntity)
        int a = random.nextInt(20) + 1;
        int b = random.nextInt(20) + 1;

        q.questionText = a + " + " + b;
        q.correctAnswer = a + b;

        // יצירת מסיחים (תשובות שגויות)
        q.options = new ArrayList<>();
        q.options.add(q.correctAnswer);
        q.options.add(q.correctAnswer + random.nextInt(5) + 1);
        q.options.add(q.correctAnswer - random.nextInt(5) - 1);
        q.options.add(q.correctAnswer + 10);

        // ערבוב התשובות כדי שהתשובה הנכונה לא תהיה תמיד הראשונה
        Collections.shuffle(q.options);

        return q;
    }
}