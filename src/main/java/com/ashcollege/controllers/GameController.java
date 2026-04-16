package com.ashcollege.controllers;

import com.ashcollege.Engine.*;
import com.ashcollege.entities.*;
import com.ashcollege.responses.*;
import com.ashcollege.service.Persist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

import static com.ashcollege.utils.Constants.*;
import static com.ashcollege.utils.Errors.*;

@RestController
public class GameController {

    @Autowired
    private Persist persist;

    @Autowired
    private ActiveGameRegistry activeGameRegistry;

    @Autowired
    private SseService sseService;

    @Autowired
    private QuestionGeneratorService questionGenerator;

    @PostMapping("/get-question")
    public BasicResponse getQuestion(@RequestBody com.ashcollege.requests.GameActionRequest request) {
        UserEntity user = persist.getUserByToken(request.getToken());
        if (user == null) return new BasicResponse(false, ERROR_WRONG_CREDENTIALS);

        ActiveGameState gameState = activeGameRegistry.getGame(request.getGameId());
        if (gameState == null) return new BasicResponse(false, ERROR_GAME_NOT_FOUND);
        if (!gameState.isRunning()) return new BasicResponse(false, ERROR_GAME_NOT_ACTIVE);
        if (gameState.isFinished()) return new BasicResponse(false, ERROR_GAME_FINISHED);

        PlayerRuntimeState playerState = gameState.getPlayers().get(user.getId());
        if (playerState == null) return new BasicResponse(false, ERROR_NO_PERMISSION);

        synchronized (gameState.getLock()) {
            MathQuestionGenerator.QuestionData qData = playerState.getCurrentQuestion();

            if (qData == null) {
                GameEntity gameEntity = persist.getGameById(request.getGameId());
                int difficulty = 0;
                if (gameEntity != null) {
                    difficulty = Math.max(0, Math.min(2, gameEntity.getGameType()));
                }

                qData = questionGenerator.generateQuestion(difficulty);
                playerState.setCurrentQuestion(qData);
                playerState.setCurrentQuestionStartTime(System.currentTimeMillis());
                playerState.setCurrentCorrectAnswer(qData.correctAnswer);
            }

            return new QuestionResponse(true, null, qData.questionText, qData.options);
        }
    }

    @PostMapping("/submit-answer")
    public BasicResponse submitAnswer(@RequestBody com.ashcollege.requests.GameActionRequest request) {
        UserEntity user = persist.getUserByToken(request.getToken());
        if (user == null) return new BasicResponse(false, ERROR_WRONG_CREDENTIALS);

        ActiveGameState gameState = activeGameRegistry.getGame(request.getGameId());
        if (gameState == null) return new BasicResponse(false, ERROR_GAME_NOT_FOUND);
        if (!gameState.isRunning()) return new BasicResponse(false, ERROR_GAME_NOT_ACTIVE);
        if (gameState.isFinished()) return new BasicResponse(false, ERROR_GAME_FINISHED);

        PlayerRuntimeState playerState = gameState.getPlayers().get(user.getId());
        if (playerState == null) return new BasicResponse(false, ERROR_NO_PERMISSION);

        synchronized (gameState.getLock()) {
            if (gameState.isFinished()) return new BasicResponse(false, ERROR_GAME_FINISHED);

            MathQuestionGenerator.QuestionData askedQuestion = playerState.getCurrentQuestion();
            if (askedQuestion == null) return new BasicResponse(false, ERROR_MISSING_VALUES);

            long timeTakenMs = System.currentTimeMillis() - playerState.getCurrentQuestionStartTime();
            boolean isCorrect = (request.getAnswer() != null && request.getAnswer() == askedQuestion.correctAnswer);
            int pointsEarned = 0;

            if (isCorrect) {
                int streakBonus = Math.min(playerState.getStreak() * 10, 50);
                int timeBonus = timeTakenMs < 5000 ? 20 : (timeTakenMs < 10000 ? 10 : 0);
                pointsEarned = 100 + streakBonus + timeBonus;

                playerState.setScore(playerState.getScore() + pointsEarned);
                playerState.setCorrectAnswers(playerState.getCorrectAnswers() + 1);
                playerState.setStreak(playerState.getStreak() + 1);

                GamePlayerModel updatedPlayer = new GamePlayerModel(user.getId(), user.getFullName(), playerState);

                Map<String, Object> eventData = new HashMap<>();
                eventData.put("type", "PLAYER_MOVED");
                eventData.put("playerId", user.getId());
                eventData.put("player", updatedPlayer);
                sseService.broadcastToGame(request.getGameId(), "gameEvent", eventData);

                playerState.setCurrentQuestion(null);

                if (playerState.getScore() >= gameState.getTrackLength()) {
                    playerState.setFinished(true);
                    finishGame(request.getGameId(), gameState, user.getId(), user.getFullName());
                }

            } else {
                playerState.setWrongAnswers(playerState.getWrongAnswers() + 1);
                playerState.setStreak(0);
            }

            QuestionLog log = new QuestionLog(
                    askedQuestion.questionText,
                    1,
                    request.getAnswer() != null ? request.getAnswer() : -1,
                    askedQuestion.correctAnswer,
                    isCorrect,
                    timeTakenMs,
                    pointsEarned
            );
            playerState.getAnswerHistory().add(log);

            return isCorrect
                    ? new BasicResponse(true, null)
                    : new BasicResponse(false, ERROR_WRONG_ANSWER);
        }
    }

    public void endGameManually(int gameId, ActiveGameState gameState) {
        synchronized (gameState.getLock()) {
            finishGame(gameId, gameState, 0, null);
        }
    }

    private void finishGame(int gameId, ActiveGameState gameState, int winnerId, String winnerName) {
        if (gameState.isFinished()) return;

        gameState.setRunning(false);
        gameState.setFinished(true);

        GameEntity game = persist.getGameById(gameId);
        if (game != null) {
            game.setStatus(FINISHED);
            game.setFinishedAt(new Date());
            persist.save(game);
        }

        List<Map<String, Object>> rankings = gameState.getPlayers().entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().getScore(), a.getValue().getScore()))
                .map(entry -> {
                    PlayerRuntimeState prs = entry.getValue();
                    Map<String, Object> playerData = new HashMap<>();
                    playerData.put("id", entry.getKey());
                    playerData.put("fullName", prs.getFullName());
                    playerData.put("score", prs.getScore());
                    playerData.put("correctAnswers", prs.getCorrectAnswers());
                    playerData.put("wrongAnswers", prs.getWrongAnswers());
                    playerData.put("streak", prs.getStreak());
                    return playerData;
                })
                .collect(Collectors.toList());

        Map<String, Object> gameOverData = new HashMap<>();
        gameOverData.put("type", "GAME_OVER");
        gameOverData.put("rankings", rankings);

        if (winnerId > 0 && winnerName != null) {
            gameOverData.put("winnerId", winnerId);
            gameOverData.put("winnerName", winnerName);
        }

        sseService.broadcastToGame(gameId, "gameEvent", gameOverData);
    }
}