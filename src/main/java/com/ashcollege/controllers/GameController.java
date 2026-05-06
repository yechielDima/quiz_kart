package com.ashcollege.controllers;

import com.ashcollege.Engine.*;
import com.ashcollege.entities.*;
import com.ashcollege.responses.*;
import com.ashcollege.service.Persist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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

    private final ScheduledExecutorService cleanupScheduler = Executors.newSingleThreadScheduledExecutor();

    private static final int[] STREAK_MILESTONES = {3, 5, 7, 10};

    public static int getTimeLimitForDifficulty(int questionDifficulty) {
        switch (questionDifficulty) {
            case QUESTION_EASY:
                return QUESTION_TIME_EASY;
            case QUESTION_HARD:
                return QUESTION_TIME_HARD;
            default:
                return QUESTION_TIME_NORMAL;
        }
    }

    private boolean isPlayerBehind(ActiveGameState gameState, int playerId) {
        Map<Integer, PlayerRuntimeState> players = gameState.getPlayers();
        if (players.size() <= 1) return false;

        PlayerRuntimeState me = players.get(playerId);
        if (me == null) return false;

        double totalScore = 0;
        for (PlayerRuntimeState p : players.values()) {
            totalScore += p.getScore();
        }
        double average = totalScore / players.size();

        return me.getScore() < average;
    }

    private int getGameType(int gameId) {
        GameEntity gameEntity = persist.getGameById(gameId);
        if (gameEntity != null) {
            return Math.max(0, Math.min(2, gameEntity.getGameType()));
        }
        return 0;
    }

    private int getRank(ActiveGameState gameState, int playerId) {
        int myScore = gameState.getPlayers().get(playerId).getScore();
        int rank = 1;
        for (PlayerRuntimeState p : gameState.getPlayers().values()) {
            if (p.getUserId() != playerId && p.getScore() > myScore) {
                rank++;
            }
        }
        return rank;
    }

    private void checkOvertakes(int gameId, ActiveGameState gameState, int playerId, String playerName, int oldScore, int newScore) {
        if (newScore <= oldScore) return;

        for (PlayerRuntimeState other : gameState.getPlayers().values()) {
            if (other.getUserId() == playerId) continue;
            if (other.getScore() < newScore && other.getScore() >= oldScore) {
                Map<String, Object> overtakeData = new HashMap<>();
                overtakeData.put("type", "OVERTAKE");
                overtakeData.put("overtakerId", playerId);
                overtakeData.put("overtakerName", playerName);
                overtakeData.put("overtakenId", other.getUserId());
                overtakeData.put("overtakenName", other.getFullName());
                overtakeData.put("newRank", getRank(gameState, playerId));
                sseService.broadcastToGame(gameId, "gameEvent", overtakeData);
            }
        }
    }

    private void checkStreakMilestone(int gameId, int playerId, String playerName, int streak) {
        for (int milestone : STREAK_MILESTONES) {
            if (streak == milestone) {
                Map<String, Object> streakData = new HashMap<>();
                streakData.put("type", "STREAK");
                streakData.put("playerId", playerId);
                streakData.put("playerName", playerName);
                streakData.put("streak", streak);
                sseService.broadcastToGame(gameId, "gameEvent", streakData);
                break;
            }
        }
    }

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
            if (playerState.isJunctionPending()) {
                return new QuestionResponse(true, null, null, null, 0, "junction", 0, false);
            }

            int questionDifficulty;
            String questionMode;
            int dirtRoadRemaining = 0;

            if (playerState.getJunctionType() == JUNCTION_AUTOSTRADA) {
                questionDifficulty = QUESTION_HARD;
                questionMode = "autostrada";
            } else if (playerState.getJunctionType() == JUNCTION_DIRT_ROAD) {
                questionDifficulty = QUESTION_EASY;
                questionMode = "dirtroad";
                dirtRoadRemaining = playerState.getDirtRoadQuestionsLeft();
            } else {
                questionDifficulty = QUESTION_NORMAL;
                questionMode = "normal";
            }

            int gameType = getGameType(request.getGameId());
            MathQuestionGenerator.QuestionData qData = playerState.getCurrentQuestion();
            int timeLimit;

            if (qData != null) {
                long elapsedMs = System.currentTimeMillis() - playerState.getCurrentQuestionStartTime();
                int totalTimeSec = getTimeLimitForDifficulty(playerState.getCurrentQuestionDifficulty());
                int remainingSec = totalTimeSec - (int)(elapsedMs / 1000);

                if (remainingSec <= 0) {
                    playerState.setWrongAnswers(playerState.getWrongAnswers() + 1);
                    playerState.setStreak(0);
                    playerState.setCurrentQuestion(null);

                    if (playerState.getJunctionType() == JUNCTION_DIRT_ROAD) {
                        playerState.setDirtRoadQuestionsLeft(playerState.getDirtRoadQuestionsLeft() - 1);
                        if (playerState.getDirtRoadQuestionsLeft() <= 0) {
                            playerState.resetJunction();
                            questionDifficulty = QUESTION_NORMAL;
                            questionMode = "normal";
                        }
                    } else if (playerState.getJunctionType() == JUNCTION_AUTOSTRADA) {
                        int newScore = Math.max(0, playerState.getScore() - AUTOSTRADA_PENALTY);
                        playerState.setScore(newScore);
                        playerState.resetJunction();
                        questionDifficulty = QUESTION_NORMAL;
                        questionMode = "normal";
                    }

                    qData = null;
                } else {
                    timeLimit = remainingSec;
                    boolean canSwap = "normal".equals(questionMode) && isPlayerBehind(gameState, user.getId());
                    return new QuestionResponse(true, null, qData.questionText, qData.options,
                            timeLimit, questionMode, dirtRoadRemaining, canSwap);
                }
            }

            if (qData == null) {
                qData = questionGenerator.generateQuestion(gameType, questionDifficulty);
                playerState.setCurrentQuestion(qData);
                playerState.setCurrentQuestionStartTime(System.currentTimeMillis());
                playerState.setCurrentCorrectAnswer(qData.correctAnswer);
                playerState.setCurrentQuestionDifficulty(questionDifficulty);
            }

            timeLimit = getTimeLimitForDifficulty(questionDifficulty);

            if (playerState.getJunctionType() == JUNCTION_DIRT_ROAD) {
                dirtRoadRemaining = playerState.getDirtRoadQuestionsLeft();
            }

            boolean canSwap = "normal".equals(questionMode) && isPlayerBehind(gameState, user.getId());

            return new QuestionResponse(true, null, qData.questionText, qData.options,
                    timeLimit, questionMode, dirtRoadRemaining, canSwap);
        }
    }

    @PostMapping("/swap-question")
    public BasicResponse swapQuestion(@RequestBody com.ashcollege.requests.GameActionRequest request) {
        UserEntity user = persist.getUserByToken(request.getToken());
        if (user == null) return new BasicResponse(false, ERROR_WRONG_CREDENTIALS);

        ActiveGameState gameState = activeGameRegistry.getGame(request.getGameId());
        if (gameState == null) return new BasicResponse(false, ERROR_GAME_NOT_FOUND);
        if (!gameState.isRunning()) return new BasicResponse(false, ERROR_GAME_NOT_ACTIVE);
        if (gameState.isFinished()) return new BasicResponse(false, ERROR_GAME_FINISHED);

        PlayerRuntimeState playerState = gameState.getPlayers().get(user.getId());
        if (playerState == null) return new BasicResponse(false, ERROR_NO_PERMISSION);

        synchronized (gameState.getLock()) {
            if (playerState.getJunctionType() != JUNCTION_NONE) {
                return new BasicResponse(false, ERROR_MISSING_VALUES);
            }

            if (!isPlayerBehind(gameState, user.getId())) {
                return new BasicResponse(false, ERROR_NO_PERMISSION);
            }

            int gameType = getGameType(request.getGameId());

            MathQuestionGenerator.QuestionData newQuestion =
                    questionGenerator.generateQuestion(gameType, QUESTION_NORMAL);

            playerState.setCurrentQuestion(newQuestion);
            playerState.setCurrentQuestionStartTime(System.currentTimeMillis());
            playerState.setCurrentCorrectAnswer(newQuestion.correctAnswer);
            playerState.setCurrentQuestionDifficulty(QUESTION_NORMAL);
            playerState.incrementSwapsUsed();

            int timeLimit = getTimeLimitForDifficulty(QUESTION_NORMAL);

            return new QuestionResponse(true, null, newQuestion.questionText, newQuestion.options,
                    timeLimit, "normal", 0, false);
        }
    }

    @PostMapping("/choose-junction")
    public BasicResponse chooseJunction(@RequestBody com.ashcollege.requests.GameActionRequest request) {
        UserEntity user = persist.getUserByToken(request.getToken());
        if (user == null) return new BasicResponse(false, ERROR_WRONG_CREDENTIALS);

        ActiveGameState gameState = activeGameRegistry.getGame(request.getGameId());
        if (gameState == null) return new BasicResponse(false, ERROR_GAME_NOT_FOUND);
        if (!gameState.isRunning()) return new BasicResponse(false, ERROR_GAME_NOT_ACTIVE);
        if (gameState.isFinished()) return new BasicResponse(false, ERROR_GAME_FINISHED);

        PlayerRuntimeState playerState = gameState.getPlayers().get(user.getId());
        if (playerState == null) return new BasicResponse(false, ERROR_NO_PERMISSION);

        synchronized (gameState.getLock()) {
            if (!playerState.isJunctionPending()) {
                return new BasicResponse(false, ERROR_MISSING_VALUES);
            }

            Integer choice = request.getChoice();
            if (choice == null || (choice != JUNCTION_AUTOSTRADA && choice != JUNCTION_DIRT_ROAD)) {
                return new BasicResponse(false, ERROR_MISSING_VALUES);
            }

            if (choice == JUNCTION_AUTOSTRADA) {
                playerState.chooseAutostrada();
            } else {
                playerState.chooseDirtRoad();
            }

            Map<String, Object> eventData = new HashMap<>();
            eventData.put("type", "JUNCTION_CHOSEN");
            eventData.put("playerId", user.getId());
            eventData.put("playerName", user.getFullName());
            eventData.put("junctionChoice", choice == JUNCTION_AUTOSTRADA ? "autostrada" : "dirtroad");
            sseService.broadcastToGame(request.getGameId(), "gameEvent", eventData);

            return new BasicResponse(true, null);
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
            int timeLimitMs = getTimeLimitForDifficulty(playerState.getCurrentQuestionDifficulty()) * 1000;
            boolean timeExpired = timeTakenMs > (timeLimitMs + 2000);

            boolean isCorrect = !timeExpired
                    && request.getAnswer() != null
                    && request.getAnswer() == askedQuestion.correctAnswer;

            int pointsEarned = 0;
            int junctionType = playerState.getJunctionType();
            int oldScore = playerState.getScore();

            if (junctionType == JUNCTION_AUTOSTRADA) {
                if (isCorrect) {
                    pointsEarned = AUTOSTRADA_REWARD;
                    playerState.setScore(playerState.getScore() + pointsEarned);
                    playerState.setCorrectAnswers(playerState.getCorrectAnswers() + 1);
                    playerState.setStreak(playerState.getStreak() + 1);
                } else {
                    int newScore = Math.max(0, playerState.getScore() - AUTOSTRADA_PENALTY);
                    playerState.setScore(newScore);
                    playerState.setWrongAnswers(playerState.getWrongAnswers() + 1);
                    playerState.setStreak(0);
                }
                playerState.setCurrentQuestion(null);
                playerState.resetJunction();

            } else if (junctionType == JUNCTION_DIRT_ROAD) {
                if (isCorrect) {
                    pointsEarned = DIRT_ROAD_POINTS;
                    playerState.setScore(playerState.getScore() + pointsEarned);
                    playerState.setCorrectAnswers(playerState.getCorrectAnswers() + 1);
                    playerState.setStreak(playerState.getStreak() + 1);
                } else {
                    playerState.setWrongAnswers(playerState.getWrongAnswers() + 1);
                    playerState.setStreak(0);
                }
                playerState.setCurrentQuestion(null);
                playerState.setDirtRoadQuestionsLeft(playerState.getDirtRoadQuestionsLeft() - 1);

                if (playerState.getDirtRoadQuestionsLeft() <= 0) {
                    playerState.resetJunction();
                }

            } else {
                if (isCorrect) {
                    int streakBonus = Math.min(playerState.getStreak() * 10, 50);
                    int timeBonus = timeTakenMs < 5000 ? 20 : (timeTakenMs < 10000 ? 10 : 0);
                    int basePoints = 100 + streakBonus + timeBonus;

                    pointsEarned = playerState.applyActiveEffect(basePoints);

                    playerState.setScore(playerState.getScore() + pointsEarned);
                    playerState.setCorrectAnswers(playerState.getCorrectAnswers() + 1);
                    playerState.setStreak(playerState.getStreak() + 1);
                    playerState.incrementDecisionMeter();
                    playerState.incrementLuckMeter();

                    playerState.setCurrentQuestion(null);

                    if (playerState.shouldTriggerJunction()) {
                        playerState.triggerJunction();
                    }

                    if (playerState.shouldTriggerLuckEvent()) {
                        String luckEvent = playerState.rollLuckEvent();
                        playerState.applyLuckEvent(luckEvent);

                        Map<String, Object> luckData = new HashMap<>();
                        luckData.put("type", "LUCK_EVENT");
                        luckData.put("playerId", user.getId());
                        luckData.put("playerName", user.getFullName());
                        luckData.put("event", luckEvent);
                        sseService.broadcastToGame(request.getGameId(), "gameEvent", luckData);
                    }
                } else {
                    playerState.setWrongAnswers(playerState.getWrongAnswers() + 1);
                    playerState.setStreak(0);

                    if (timeExpired) {
                        playerState.setCurrentQuestion(null);
                    }
                }
            }

            GamePlayerModel updatedPlayer = new GamePlayerModel(user.getId(), user.getFullName(), playerState);

            Map<String, Object> eventData = new HashMap<>();
            eventData.put("type", "PLAYER_MOVED");
            eventData.put("playerId", user.getId());
            eventData.put("player", updatedPlayer);
            sseService.broadcastToGame(request.getGameId(), "gameEvent", eventData);

            if (isCorrect) {
                checkOvertakes(request.getGameId(), gameState, user.getId(), user.getFullName(), oldScore, playerState.getScore());
                checkStreakMilestone(request.getGameId(), user.getId(), user.getFullName(), playerState.getStreak());
            }

            if (playerState.getScore() >= gameState.getTrackLength()) {
                playerState.setFinished(true);
                finishGame(request.getGameId(), gameState, user.getId(), user.getFullName());
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
                    int totalAnswers = prs.getCorrectAnswers() + prs.getWrongAnswers();

                    Map<String, Object> playerData = new HashMap<>();
                    playerData.put("id", entry.getKey());
                    playerData.put("fullName", prs.getFullName());
                    playerData.put("score", prs.getScore());
                    playerData.put("correctAnswers", prs.getCorrectAnswers());
                    playerData.put("wrongAnswers", prs.getWrongAnswers());
                    playerData.put("bestStreak", prs.getBestStreak());
                    playerData.put("totalAnswers", totalAnswers);
                    playerData.put("successRate", totalAnswers > 0 ? Math.round((double) prs.getCorrectAnswers() / totalAnswers * 100) : 0);
                    playerData.put("avgTimeSec", Math.round(prs.getAverageAnswerTimeMs() / 100.0) / 10.0);
                    playerData.put("luckEvents", prs.getLuckEventsReceived());
                    playerData.put("swapsUsed", prs.getSwapsUsed());
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

        cleanupScheduler.schedule(() -> {
            activeGameRegistry.removeGame(gameId);
            sseService.cleanupGame(gameId);
        }, 2, TimeUnit.MINUTES);
    }
}