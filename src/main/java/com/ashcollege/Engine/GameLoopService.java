package com.ashcollege.Engine;

import com.ashcollege.entities.GamePlayerEntity;
import com.ashcollege.entities.PlayerAnswerEntity;
import com.ashcollege.service.Persist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
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
        scheduler.scheduleAtFixedRate(this::syncStateToDatabase, 5, 5, TimeUnit.SECONDS);
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
                            gp.setWrongAnswers(liveState.getWrongAnswers());
                            gp.setStreak(liveState.getStreak());
                            gp.setFinished(liveState.isFinished());
                            persist.save(gp);

                            List<QuestionLog> history = liveState.getAnswerHistory();

                            if (!history.isEmpty()) {
                                List<QuestionLog> historyCopy = new ArrayList<>(history);

                                for (QuestionLog log : historyCopy) {
                                    PlayerAnswerEntity answerEntity = new PlayerAnswerEntity();
                                    answerEntity.setGamePlayer(gp);
                                    answerEntity.setQuestionText(log.getQuestionText());
                                    answerEntity.setQuestionType(log.getQuestionType());
                                    answerEntity.setPlayerAnswer(log.getPlayerAnswer());
                                    answerEntity.setCorrectAnswer(log.getCorrectAnswer());
                                    answerEntity.setCorrect(log.isCorrect());
                                    answerEntity.setTimeTakenMs(log.getTimeTakenMs());
                                    answerEntity.setPointsEarned(log.getPointsEarned());
                                    persist.save(answerEntity);
                                }

                                history.removeAll(historyCopy);
                            }
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