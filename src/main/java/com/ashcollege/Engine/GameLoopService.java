package com.ashcollege.Engine;

import com.ashcollege.entities.GameEntity;
import com.ashcollege.entities.GamePlayerEntity;
import com.ashcollege.service.Persist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class GameLoopService {

    private final ActiveGameRegistry activeGameRegistry;
    private final Persist persist;

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    @Autowired
    public GameLoopService(ActiveGameRegistry activeGameRegistry, Persist persist) {
        this.activeGameRegistry = activeGameRegistry;
        this.persist = persist;
    }

    @PostConstruct
    public void startLoop() {
        scheduler.scheduleAtFixedRate(this::tickAllGames, 0, 200, TimeUnit.MILLISECONDS);
    }

    private void tickAllGames() {
        for (ActiveGameState gameState : activeGameRegistry.getAllGames()) {
            try {
                System.out.println("tick");
                tickGame(gameState);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void tickGame(ActiveGameState gameState) {
        synchronized (gameState.getLock()) {
            if (!gameState.isRunning() || gameState.isFinished()) {
                return;
            }

            boolean hasWinner = false;
            int winnerUserId = -1;

            for (PlayerRuntimeState playerState : gameState.getPlayers().values()) {
                if (playerState.getScore() >= gameState.getTrackLength()) {
                    playerState.setFinished(true);
                    hasWinner = true;
                    winnerUserId = playerState.getUserId();
                    break;
                }
            }

            if (hasWinner) {
                finishGame(gameState, winnerUserId);
            }

            gameState.setLastTickTime(System.currentTimeMillis());
        }
    }

    private void finishGame(ActiveGameState gameState, int winnerUserId) {
        gameState.setRunning(false);
        gameState.setFinished(true);

        GameEntity game = persist.getGameById(gameState.getGameId());
        if (game != null) {
            game.setStatus(2); // FINISHED
            game.setFinishedAt(new Timestamp(System.currentTimeMillis()));
            persist.save(game);
        }

        List<GamePlayerEntity> gamePlayers = persist.getGamePlayersByGameId(gameState.getGameId());
        if (gamePlayers != null) {
            for (GamePlayerEntity gp : gamePlayers) {
                PlayerRuntimeState playerState = gameState.getPlayers().get(gp.getPlayer().getId());
                if (playerState != null) {
                    gp.setScore(playerState.getScore());
                    gp.setCorrectAnswers(playerState.getCorrectAnswers());
                    gp.setWrongAnswers(playerState.getWrongAnswers());
                    gp.setStreak(playerState.getStreak());
                    gp.setFinished(playerState.isFinished());

                    if (gp.getPlayer().getId() == winnerUserId) {
                        gp.setFinished(true);
                        gp.setFinishTime(new Timestamp(System.currentTimeMillis()));
                    }

                    persist.save(gp);
                }
            }
        }

        activeGameRegistry.removeGame(gameState.getGameId());
        System.out.println("Game finished: " + gameState.getGameId() + ", winner user id: " + winnerUserId);
    }

    @PreDestroy
    public void stopLoop() {
        scheduler.shutdown();
    }
}

