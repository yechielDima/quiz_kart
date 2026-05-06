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
                            int score;
                            int correct;
                            int wrong;
                            int streak;
                            boolean finished;
                            List<QuestionLog> historyCopy;

                            synchronized (gameState.getLock()) {
                                score = liveState.getScore();
                                correct = liveState.getCorrectAnswers();
                                wrong = liveState.getWrongAnswers();
                                streak = liveState.getStreak();
                                finished = liveState.isFinished();
                                historyCopy = new ArrayList<>(liveState.getAnswerHistory());
                                liveState.getAnswerHistory().clear();
                            }

                            gp.setScore(score);
                            gp.setCorrectAnswers(correct);
                            gp.setWrongAnswers(wrong);
                            gp.setStreak(streak);
                            gp.setFinished(finished);
                            persist.save(gp);

                            if (!historyCopy.isEmpty()) {
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