package com.ashcollege.controllers;

import com.ashcollege.Engine.*;
import com.ashcollege.entities.*;
import com.ashcollege.responses.*;
import com.ashcollege.service.Persist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.annotation.PostConstruct;

import java.util.*;

import static com.ashcollege.utils.Errors.*;

@RestController
public class GameController {

    @Autowired
    private Persist persist;

    @Autowired
    private ActiveGameRegistry activeGameRegistry;

    @Autowired
    private SseService sseService;

    @PostConstruct
    public void init() {
    }

    @RequestMapping("/get-question")
    public BasicResponse getQuestion(String token, int gameId) {
        UserEntity user = persist.getUserByToken(token);
        if (user == null) return new BasicResponse(false, ERROR_WRONG_CREDENTIALS);

        ActiveGameState gameState = activeGameRegistry.getGame(gameId);
        if (gameState == null) {
            return new BasicResponse(false, ERROR_GAME_NOT_FOUND); // המשחק לא קיים
        }
        if(!gameState.isRunning()){
            return new BasicResponse(false, ERROR_GAME_NOT_ACTIVE); // המשחק לא רץ

        }

        PlayerRuntimeState playerState = gameState.getPlayers().get(user.getId());
        if (playerState == null) return new BasicResponse(false, ERROR_NO_PREMITION);


        // --- התיקון הקריטי למניעת ריענון: נבדוק אם כבר יש שאלה! ---
        MathQuestionGenerator.QuestionData qData = playerState.getCurrentQuestion();

        if (qData == null) {
            // רק אם אין שאלה פעילה, נגריל אחת חדשה
            /*כשנשנה את היוצר שאלות אז צריך לזכור להכניס פה פרמטרים*/

            qData = MathQuestionGenerator.generateQuestion();
            playerState.setCurrentQuestion(qData);
            playerState.setCurrentQuestionStartTime(System.currentTimeMillis());
            playerState.setCurrentCorrectAnswer(qData.correctAnswer);
        }

        // שולחים ללקוח את השאלה (בין אם היא חדשה ובין אם זו אותה אחת מקודם)
        return new QuestionResponse(true, null, qData.questionText, qData.options);
    }

    @RequestMapping("/submit-answer")
    public BasicResponse submitAnswer(String token, int gameId, int answer) {
        // 1. אימות ראשוני
        UserEntity user = persist.getUserByToken(token);
        if (user == null) return new BasicResponse(false, ERROR_WRONG_CREDENTIALS);

        ActiveGameState gameState = activeGameRegistry.getGame(gameId);
        if (gameState == null) {
            return new BasicResponse(false, ERROR_GAME_NOT_FOUND); // המשחק לא קיים
        }
        if(!gameState.isRunning()){
            return new BasicResponse(false, ERROR_GAME_NOT_ACTIVE); // המשחק לא רץ

        }
        PlayerRuntimeState playerState = gameState.getPlayers().get(user.getId());
        if (playerState == null) return new BasicResponse(false, ERROR_NO_PREMITION);

        // 2. משיכת השאלה והזמן
        MathQuestionGenerator.QuestionData askedQuestion = playerState.getCurrentQuestion();
        if (askedQuestion == null) return new BasicResponse(false, ERROR_MISSING_VALUES); // אין שאלה פעילה

        // 3. עצירת השעון
        long timeTakenMs = System.currentTimeMillis() - playerState.getCurrentQuestionStartTime();

        // 4. בדיקה וניקוד
        boolean isCorrect = (answer == askedQuestion.correctAnswer);
        int pointsEarned = 0;

        if (isCorrect) {
            pointsEarned = 100; // תמיד אפשר לעשות פה בונוס (למשל אם timeTakenMs < 3000 לתת 2 נקודות)
            playerState.setScore(playerState.getScore() + pointsEarned);
            playerState.setCorrectAnswers(playerState.getCorrectAnswers() + 1);

            // 5. שידור לכולם ב-SSE כדי להזיז את המכונית
            GamePlayerModel updatedPlayer = new GamePlayerModel(playerState);
            Map<String, Object> eventData = new java.util.HashMap<>();
            eventData.put("type", "PLAYER_MOVED");
            eventData.put("player", updatedPlayer);

            sseService.broadcastToGame(gameId, "gameEvent", eventData);
        } else {
            // עדכון של כמות הטעויות בזיכרון
            playerState.setWrongAnswers(playerState.getWrongAnswers() + 1);
        }

        // 6. תיעוד ההיסטוריה למורה/מנהל (לשמירה עתידית ב-DB)
        QuestionLog log = new QuestionLog(
                askedQuestion.questionText,
                1, // סוג שאלה (נניח 1 זה חיבור)
                answer,
                askedQuestion.correctAnswer,
                isCorrect,
                timeTakenMs,
                pointsEarned
        );
        playerState.getAnswerHistory().add(log);

        // 7. איפוס השאלה כדי למנוע מענה כפול או רמאות
        playerState.setCurrentQuestion(null);

        // מחזירים ללקוח אם ענה נכון או לא (כדי להראות לו "כל הכבוד!" או "טעות!")
        if (isCorrect) {
            return new BasicResponse(true, null);
        } else {
            return new BasicResponse(false, ERROR_WRONG_ANSWER); // ודא שיש לך קבוע ERROR_WRONG_ANSWER ב-Errors.java
        }
    }
}