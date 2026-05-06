package com.ashcollege.Engine;

import com.ashcollege.entities.QuestionTemplateEntity;
import com.ashcollege.entities.QuestionWordEntity;
import com.ashcollege.service.Persist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static com.ashcollege.utils.Constants.*;

@Service
public class QuestionGeneratorService {

    @Autowired
    private Persist persist;

    private final Random random = new Random();

    private volatile List<QuestionTemplateEntity> cachedTemplates = new ArrayList<>();
    private volatile List<String> cachedNames = new ArrayList<>();
    private volatile List<String> cachedObjects = new ArrayList<>();

    private static final int[][] ADD_SUB_MAX = {
            {10, 15, 20},
            {20, 40, 60},
            {50, 80, 100}
    };

    private static final int[][] MUL_DIV_MAX = {
            {5, 7, 8},
            {10, 12, 15},
            {13, 16, 20}
    };

    private static final int[][] PERCENT_OPTIONS = {
            {50, 10},
            {10, 20, 25, 50},
            {5, 10, 15, 20, 25, 50, 75}
    };

    private static final int[][] PERCENT_RESULT_RANGE = {
            {1, 5},
            {1, 10},
            {5, 20}
    };

    @PostConstruct
    public void loadFromDatabase() {
        refreshCache();
    }

    public void refreshCache() {
        List<QuestionTemplateEntity> newTemplates = persist.getAllTemplates();
        List<QuestionWordEntity> allWords = persist.getAllWords();

        List<String> newNames = allWords.stream()
                .filter(w -> "name".equals(w.getCategory()))
                .map(QuestionWordEntity::getWord)
                .collect(Collectors.toList());

        List<String> newObjects = allWords.stream()
                .filter(w -> "object".equals(w.getCategory()))
                .map(QuestionWordEntity::getWord)
                .collect(Collectors.toList());

        cachedTemplates = newTemplates;
        cachedNames = newNames;
        cachedObjects = newObjects;
    }

    public MathQuestionGenerator.QuestionData generateQuestion(int gameType, int questionDifficulty) {
        MathQuestionGenerator.QuestionData qData = new MathQuestionGenerator.QuestionData();

        int operationType = chooseOperationByGameType(gameType);

        int num1 = 0, num2 = 0, correctAnswer = 0;
        String sign = "";

        int qd = Math.max(0, Math.min(2, questionDifficulty));
        int gt = Math.max(0, Math.min(2, gameType));

        switch (operationType) {
            case 0:
                int addMax = ADD_SUB_MAX[qd][gt];
                num1 = randomInRange(1, addMax);
                num2 = randomInRange(1, addMax);
                correctAnswer = num1 + num2;
                sign = "+";
                break;
            case 1:
                int subMax = ADD_SUB_MAX[qd][gt];
                num1 = randomInRange(3, subMax + 3);
                num2 = randomInRange(1, num1 - 1);
                correctAnswer = num1 - num2;
                sign = "-";
                break;
            case 2:
                int mulMax = MUL_DIV_MAX[qd][gt];
                num1 = randomInRange(2, mulMax);
                num2 = randomInRange(2, mulMax);
                correctAnswer = num1 * num2;
                sign = "×";
                break;
            case 3:
                int divMax = MUL_DIV_MAX[qd][gt];
                num2 = randomInRange(2, divMax);
                correctAnswer = randomInRange(2, divMax);
                num1 = num2 * correctAnswer;
                sign = "÷";
                break;
            case 4:
                int[] percentages = PERCENT_OPTIONS[qd];
                int pMin = PERCENT_RESULT_RANGE[qd][0];
                int pMax = PERCENT_RESULT_RANGE[qd][1];
                num1 = percentages[random.nextInt(percentages.length)];
                correctAnswer = randomInRange(pMin, pMax);
                num2 = correctAnswer * 100 / num1;
                sign = "% מתוך";
                break;
        }

        qData.questionText = buildQuestionText(operationType, gt, num1, num2, sign);
        qData.correctAnswer = correctAnswer;
        qData.options = generateOptions(correctAnswer, operationType, num1, num2);

        return qData;
    }

    private String buildQuestionText(int operationType, int gameType, int num1, int num2, String sign) {
        List<QuestionTemplateEntity> templates = cachedTemplates;
        List<String> names = cachedNames;
        List<String> objects = cachedObjects;

        boolean hasTemplates = !templates.isEmpty();
        boolean hasNames = !names.isEmpty();
        boolean hasObjects = !objects.isEmpty();

        if (hasTemplates && hasNames && hasObjects && random.nextBoolean()) {
            List<QuestionTemplateEntity> matching = templates.stream()
                    .filter(t -> t.getOperationType() == operationType && t.getDifficultyLevel() <= gameType)
                    .collect(Collectors.toList());

            if (!matching.isEmpty()) {
                QuestionTemplateEntity template = matching.get(random.nextInt(matching.size()));
                String text = template.getTemplateText();

                String name1 = names.get(random.nextInt(names.size()));
                String name2 = names.get(random.nextInt(names.size()));
                while (name2.equals(name1) && names.size() > 1) {
                    name2 = names.get(random.nextInt(names.size()));
                }

                String obj = objects.get(random.nextInt(objects.size()));

                text = text.replace("{num1}", String.valueOf(num1))
                        .replace("{num2}", String.valueOf(num2))
                        .replace("{name1}", name1)
                        .replace("{name2}", name2)
                        .replace("{obj}", obj);

                return text;
            }
        }

        return num1 + " " + sign + " " + num2;
    }

    private List<Integer> generateOptions(int correctAnswer, int operationType, int num1, int num2) {
        List<Integer> options = new ArrayList<>();
        options.add(correctAnswer);

        int maxAttempts = 50;
        int attempts = 0;

        while (options.size() < 4 && attempts < maxAttempts) {
            attempts++;
            int fakeAnswer = generateFakeAnswer(correctAnswer, operationType, num1, num2);

            if (fakeAnswer >= 0 && fakeAnswer != correctAnswer && !options.contains(fakeAnswer)) {
                options.add(fakeAnswer);
            }
        }

        int fallbackOffset = 1;
        while (options.size() < 4) {
            int fallback = correctAnswer + fallbackOffset;
            if (fallback >= 0 && !options.contains(fallback)) {
                options.add(fallback);
            }
            fallbackOffset = (fallbackOffset > 0) ? -fallbackOffset : -fallbackOffset + 1;
        }

        Collections.shuffle(options);
        return options;
    }

    private int generateFakeAnswer(int correctAnswer, int operationType, int num1, int num2) {
        switch (operationType) {
            case 2:
                int wrongFactor = random.nextBoolean() ? num1 : num2;
                return correctAnswer + (random.nextBoolean() ? wrongFactor : -wrongFactor);
            case 3:
                int divShift = randomInRange(1, 3);
                return random.nextBoolean()
                        ? correctAnswer + divShift
                        : correctAnswer - divShift;
            case 4:
                int percentShift = randomInRange(1, Math.max(2, correctAnswer));
                return correctAnswer + (random.nextBoolean() ? percentShift : -percentShift);
            default:
                int shift = randomInRange(1, 5);
                return correctAnswer + (random.nextBoolean() ? shift : -shift);
        }
    }

    private int chooseOperationByGameType(int gameType) {
        if (gameType == 0) return random.nextInt(2);
        if (gameType == 1) return random.nextInt(4);
        return random.nextInt(5);
    }

    private int randomInRange(int min, int max) {
        if (min >= max) return min;
        return random.nextInt(max - min + 1) + min;
    }
}