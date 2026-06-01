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

    private void broadcastPlayerMoved(int gameId, int playerId, String playerName, PlayerRuntimeState playerState) {
        GamePlayerModel updatedPlayer = new GamePlayerModel(playerId, playerName, playerState);

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("type", "PLAYER_MOVED");
        eventData.put("playerId", playerId);
        eventData.put("player", updatedPlayer);
        sseService.broadcastToGame(gameId, "gameEvent", eventData);
    }

    private List<Integer> trimOptions(List<Integer> options, int correctAnswer, int targetCount) {
        if (targetCount >= options.size()) return options;

        List<Integer> wrong = new ArrayList<>();
        for (Integer opt : options) {
            if (opt != correctAnswer) {
                wrong.add(opt);
            }
        }
        Collections.shuffle(wrong);

        List<Integer> trimmed = new ArrayList<>();
        trimmed.add(correctAnswer);
        for (int i = 0; i < targetCount - 1 && i < wrong.size(); i++) {
            trimmed.add(wrong.get(i));
        }
        Collections.shuffle(trimmed);
        return trimmed;
    }
    @PostMapping("/leave-game")
    public BasicResponse leaveGame(@RequestBody com.ashcollege.requests.GameActionRequest request) {
        UserEntity user = persist.getUserByToken(request.getToken());
        if (user == null) return new BasicResponse(false, ERROR_WRONG_CREDENTIALS);

        GameEntity game = persist.getGameById(request.getGameId());
        if (game == null) return new BasicResponse(false, ERROR_GAME_NOT_FOUND);

        ActiveGameState activeGame = activeGameRegistry.getGame(game.getId());

        if (game.getStatus() != WAITING) {
            return new BasicResponse(true, null);
        }

        if (game.getCreator().getId() == user.getId()) {
            game.setStatus(FINISHED);
            game.setDeleted(true);
            persist.save(game);
            persist.flush();

            if (activeGame != null) {
                activeGameRegistry.removeGame(game.getId());
            }

            Map<String, Object> eventData = new HashMap<>();
            eventData.put("type", "GAME_CANCELLED");
            sseService.broadcastToGame(game.getId(), "gameEvent", eventData);

            sseService.cleanupGame(game.getId());

            return new BasicResponse(true, null);
        }

        GamePlayerEntity gp = persist.getGamePlayerByGameAndUser(game.getId(), user.getId());
        if (gp != null) {
            gp.setDeleted(true);
            persist.save(gp);
            persist.flush();

            if (activeGame != null) {
                activeGame.getPlayers().remove(user.getId());

                List<GamePlayerModel> livePlayers = new ArrayList<>();
                for (PlayerRuntimeState prs : activeGame.getPlayers().values()) {
                    livePlayers.add(new GamePlayerModel(prs.getUserId(), prs.getFullName(), prs));
                }

                Map<String, Object> updateEvent = new HashMap<>();
                updateEvent.put("type", "PLAYERS_LIST_UPDATE");
                updateEvent.put("players", livePlayers);
                sseService.broadcastToGame(game.getId(), "gameEvent", updateEvent);
            }
        }

        return new BasicResponse(true, null);
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
                return new QuestionResponse(true, null, null, null, 0, "junction", 0);
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
                    int oldScore = playerState.getScore();

                    playerState.setWrongAnswers(playerState.getWrongAnswers() + 1);
                    playerState.setStreak(0);
                    playerState.setCurrentQuestion(null);

                    if ("normal".equals(questionMode)) {
                        playerState.incrementConsecutiveWrong();
                    }

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

                    if (playerState.getScore() != oldScore) {
                        broadcastPlayerMoved(request.getGameId(), user.getId(), user.getFullName(), playerState);
                    }

                    qData = null;
                } else {
                    timeLimit = remainingSec;
                    List<Integer> finalOptions = qData.options;
                    if ("normal".equals(questionMode)) {
                        int optionCount = playerState.getOptionCount();
                        finalOptions = trimOptions(qData.options, qData.correctAnswer, optionCount);
                    }
                    return new QuestionResponse(true, null, qData.questionText, finalOptions,
                            timeLimit, questionMode, dirtRoadRemaining);
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

            List<Integer> finalOptions = qData.options;
            if ("normal".equals(questionMode)) {
                int optionCount = playerState.getOptionCount();
                finalOptions = trimOptions(qData.options, qData.correctAnswer, optionCount);
            }

            return new QuestionResponse(true, null, qData.questionText, finalOptions,
                    timeLimit, questionMode, dirtRoadRemaining);
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
            boolean timeExpired = timeTakenMs > (timeLimitMs + ANSWER_GRACE_MS);

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
                    int streakBonus = Math.min(playerState.getStreak() * NORMAL_STREAK_BONUS, NORMAL_MAX_STREAK_BONUS);
                    int timeBonus = timeTakenMs < 5000 ? NORMAL_FAST_TIME_BONUS : (timeTakenMs < 10000 ? NORMAL_MEDIUM_TIME_BONUS : 0);
                    int basePoints = NORMAL_BASE_POINTS + streakBonus + timeBonus;

                    pointsEarned = playerState.applyActiveEffect(basePoints);

                    playerState.setScore(playerState.getScore() + pointsEarned);
                    playerState.setCorrectAnswers(playerState.getCorrectAnswers() + 1);
                    playerState.setStreak(playerState.getStreak() + 1);
                    playerState.incrementDecisionMeter();
                    playerState.incrementLuckMeter();
                    playerState.resetConsecutiveWrong();

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
                    playerState.incrementConsecutiveWrong();
                }

                playerState.setCurrentQuestion(null);
            }

            int newScore = playerState.getScore();

            if (oldScore != newScore) {
                broadcastPlayerMoved(request.getGameId(), user.getId(), user.getFullName(), playerState);

                if (isCorrect) {
                    checkOvertakes(request.getGameId(), gameState, user.getId(), user.getFullName(), oldScore, newScore);
                    checkStreakMilestone(request.getGameId(), user.getId(), user.getFullName(), playerState.getStreak());
                }

                if (newScore >= gameState.getTrackLength()) {
                    playerState.setFinished(true);
                    finishGame(request.getGameId(), gameState, user.getId(), user.getFullName());
                }
            }

            playerState.recordAnswerTime(timeTakenMs);

            QuestionLog log = new QuestionLog(
                    askedQuestion.questionText,
                    askedQuestion.operationType,
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