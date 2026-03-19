package com.ashcollege.Engine;

import com.ashcollege.entities.GamePlayerEntity;
import com.ashcollege.service.Persist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class GameLoopService {

    private final ActiveGameRegistry activeGameRegistry;
    private final Persist persist;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Autowired
    public GameLoopService(ActiveGameRegistry activeGameRegistry, Persist persist) {
        this.activeGameRegistry = activeGameRegistry;
        this.persist = persist;
    }

    @PostConstruct
    public void startLoop() {
        // הלופ שומר את הסטייט בד"ב כל חמש שניות
        scheduler.scheduleAtFixedRate(this::syncStateToDatabase, 0, 5000, TimeUnit.MILLISECONDS);
    }

    private void syncStateToDatabase() {
        for (ActiveGameState gameState : activeGameRegistry.getAllGames()) {
            try {
                List<GamePlayerEntity> dbPlayers = persist.getGamePlayersByGameId(gameState.getGameId());

                if (dbPlayers != null) {
                    for (GamePlayerEntity gp : dbPlayers) {
                        PlayerRuntimeState liveState = gameState.getPlayers().get(gp.getPlayer().getId());
                        if (liveState != null) {
                            gp.setScore(liveState.getScore());
                            gp.setCorrectAnswers(liveState.getCorrectAnswers());
                            // נצטרך להוסיף פה עוד שדות כדי לעדכן הכל בד"ב כולל נקודות וסטרייקים וכל מה שיש ב GamePlayerEntity
                            persist.save(gp);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @PreDestroy
    public void stopLoop() {
        scheduler.shutdown();
    }
}