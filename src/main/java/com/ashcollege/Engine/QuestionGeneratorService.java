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

@Service
public class QuestionGeneratorService {

    @Autowired
    private Persist persist;

    private final Random random = new Random();

    private List<QuestionTemplateEntity> cachedTemplates = new ArrayList<>();
    private List<String> cachedNames = new ArrayList<>();
    private List<String> cachedObjects = new ArrayList<>();

    @PostConstruct
    public void loadFromDatabase() {
        refreshCache();
    }

    public void refreshCache() {
        cachedTemplates = persist.getAllTemplates();

        List<QuestionWordEntity> allWords = persist.getAllWords();
        cachedNames = allWords.stream()
                .filter(w -> "name".equals(w.getCategory()))
                .map(QuestionWordEntity::getWord)
                .collect(Collectors.toList());
        cachedObjects = allWords.stream()
                .filter(w -> "object".equals(w.getCategory()))
                .map(QuestionWordEntity::getWord)
                .collect(Collectors.toList());
    }

    public MathQuestionGenerator.QuestionData generateQuestion(int difficultyLevel) {
        MathQuestionGenerator.QuestionData qData = new MathQuestionGenerator.QuestionData();

        int operationType = chooseOperationByDifficulty(difficultyLevel);

        int num1 = 0, num2 = 0, correctAnswer = 0;
        String sign = "";

        switch (operationType) {
            case 0:
                num1 = randomInRange(1, (difficultyLevel + 1) * 20);
                num2 = randomInRange(1, (difficultyLevel + 1) * 20);
                correctAnswer = num1 + num2;
                sign = "+";
                break;
            case 1:
                num1 = randomInRange(5, (difficultyLevel + 1) * 20 + 5);
                num2 = randomInRange(1, num1 - 1);
                correctAnswer = num1 - num2;
                sign = "-";
                break;
            case 2:
                int mulMax = (difficultyLevel == 0) ? 10 : (difficultyLevel == 1) ? 12 : 15;
                num1 = randomInRange(2, mulMax);
                num2 = randomInRange(2, mulMax);
                correctAnswer = num1 * num2;
                sign = "×";
                break;
            case 3:
                int divMax = (difficultyLevel == 0) ? 10 : (difficultyLevel == 1) ? 12 : 15;
                num2 = randomInRange(2, divMax);
                correctAnswer = randomInRange(2, divMax);
                num1 = num2 * correctAnswer;
                sign = "÷";
                break;
            case 4:
                int[] nicePercentages = {10, 20, 25, 50};
                num1 = nicePercentages[random.nextInt(nicePercentages.length)];
                correctAnswer = randomInRange(1, 10);
                num2 = correctAnswer * 100 / num1;
                sign = "% מתוך";
                break;
        }

        qData.questionText = buildQuestionText(operationType, difficultyLevel, num1, num2, sign);
        qData.correctAnswer = correctAnswer;
        qData.options = generateOptions(correctAnswer, operationType, num1, num2);

        return qData;
    }

    private String buildQuestionText(int operationType, int difficultyLevel, int num1, int num2, String sign) {
        boolean hasTemplates = !cachedTemplates.isEmpty();
        boolean hasNames = !cachedNames.isEmpty();
        boolean hasObjects = !cachedObjects.isEmpty();

        if (hasTemplates && hasNames && hasObjects && random.nextBoolean()) {
            List<QuestionTemplateEntity> matching = cachedTemplates.stream()
                    .filter(t -> t.getOperationType() == operationType && t.getDifficultyLevel() <= difficultyLevel)
                    .collect(Collectors.toList());

            if (!matching.isEmpty()) {
                QuestionTemplateEntity template = matching.get(random.nextInt(matching.size()));
                String text = template.getTemplateText();

                String name1 = cachedNames.get(random.nextInt(cachedNames.size()));
                String name2 = cachedNames.get(random.nextInt(cachedNames.size()));
                while (name2.equals(name1) && cachedNames.size() > 1) {
                    name2 = cachedNames.get(random.nextInt(cachedNames.size()));
                }

                String obj = cachedObjects.get(random.nextInt(cachedObjects.size()));

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

    private int chooseOperationByDifficulty(int level) {
        if (level == 0) return random.nextInt(2);
        if (level == 1) return random.nextInt(4);
        return random.nextInt(5);
    }

    private int randomInRange(int min, int max) {
        if (min >= max) return min;
        return random.nextInt(max - min + 1) + min;
    }
}