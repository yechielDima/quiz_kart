package com.ashcollege.controllers;

import com.ashcollege.entities.*;
import com.ashcollege.responses.BasicResponse;
import com.ashcollege.responses.StatsResponse;
import com.ashcollege.service.Persist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

import static com.ashcollege.utils.Errors.*;

@RestController
public class StatsController {

    @Autowired
    private Persist persist;

    private static final String[] OPERATION_NAMES = {"חיבור", "חיסור", "כפל", "חילוק", "אחוזים"};

    @GetMapping("/get-player-stats")
    public BasicResponse getPlayerStats(String token) {
        UserEntity user = persist.getUserByToken(token);
        if (user == null) return new BasicResponse(false, ERROR_WRONG_CREDENTIALS);

        List<GamePlayerEntity> gamePlayers = persist.getGamePlayersByUserId(user.getId());
        List<PlayerAnswerEntity> allAnswers = persist.getAllAnswersByUserId(user.getId());

        int totalGames = gamePlayers.size();
        int gamesWon = 0;
        int totalScore = 0;

        List<Map<String, Object>> recentGames = new ArrayList<>();

        for (GamePlayerEntity gp : gamePlayers) {
            totalScore += gp.getScore();

            GameEntity game = gp.getGame();
            if (game != null && game.getStatus() == 2) {
                List<GamePlayerEntity> allPlayersInGame = persist.getGamePlayersByGameId(game.getId());
                boolean isWinner = true;
                for (GamePlayerEntity other : allPlayersInGame) {
                    if (other.getPlayer().getId() != user.getId() && other.getScore() > gp.getScore()) {
                        isWinner = false;
                        break;
                    }
                }
                if (isWinner && allPlayersInGame.size() > 1) {
                    gamesWon++;
                }
            }

            if (recentGames.size() < 10 && game != null) {
                Map<String, Object> gameInfo = new HashMap<>();
                gameInfo.put("gameName", game.getGameName());
                gameInfo.put("score", gp.getScore());
                gameInfo.put("correctAnswers", gp.getCorrectAnswers());
                gameInfo.put("wrongAnswers", gp.getWrongAnswers());
                gameInfo.put("finished", game.getStatus() == 2);
                gameInfo.put("date", game.getStartedAt());
                recentGames.add(gameInfo);
            }
        }

        int totalAnswers = allAnswers.size();
        int correctAnswers = 0;
        long totalTimeMs = 0;

        int[] opTotal = new int[5];
        int[] opCorrect = new int[5];
        long[] opTimeMs = new long[5];

        for (PlayerAnswerEntity answer : allAnswers) {
            if (answer.isCorrect()) correctAnswers++;
            totalTimeMs += answer.getTimeTakenMs();

            int op = answer.getQuestionType();
            if (op >= 0 && op < 5) {
                opTotal[op]++;
                if (answer.isCorrect()) opCorrect[op]++;
                opTimeMs[op] += answer.getTimeTakenMs();
            }
        }

        double avgTimeSec = totalAnswers > 0 ? Math.round((double) totalTimeMs / totalAnswers / 100.0) / 10.0 : 0;
        int successRate = totalAnswers > 0 ? (int) Math.round((double) correctAnswers / totalAnswers * 100) : 0;

        List<Map<String, Object>> operationStats = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            if (opTotal[i] > 0) {
                Map<String, Object> opStat = new HashMap<>();
                opStat.put("name", OPERATION_NAMES[i]);
                opStat.put("total", opTotal[i]);
                opStat.put("correct", opCorrect[i]);
                opStat.put("successRate", (int) Math.round((double) opCorrect[i] / opTotal[i] * 100));
                opStat.put("avgTimeSec", Math.round((double) opTimeMs[i] / opTotal[i] / 100.0) / 10.0);
                operationStats.add(opStat);
            }
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalGames", totalGames);
        stats.put("gamesWon", gamesWon);
        stats.put("totalScore", totalScore);
        stats.put("totalAnswers", totalAnswers);
        stats.put("correctAnswers", correctAnswers);
        stats.put("wrongAnswers", totalAnswers - correctAnswers);
        stats.put("successRate", successRate);
        stats.put("avgTimeSec", avgTimeSec);
        stats.put("operationStats", operationStats);
        stats.put("recentGames", recentGames);

        return new StatsResponse(true, null, stats);
    }
}