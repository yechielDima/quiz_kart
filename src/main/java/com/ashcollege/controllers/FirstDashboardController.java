package com.ashcollege.controllers;

import com.ashcollege.Engine.ActiveGameRegistry;
import com.ashcollege.Engine.ActiveGameState;
import com.ashcollege.Engine.PlayerRuntimeState;
import com.ashcollege.entities.GameEntity;
import com.ashcollege.entities.GamePlayerEntity;
import com.ashcollege.entities.UserEntity;
import com.ashcollege.responses.*;
import com.ashcollege.service.Persist;
import com.ashcollege.utils.GeneralUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;

import java.util.Date;
import java.util.List;

import static com.ashcollege.utils.Constants.*;
import static com.ashcollege.utils.Errors.*;
import java.util.HashMap;
import java.util.Map;
@RestController
public class FirstDashboardController {
    @Autowired
    private Persist persist;
    @Autowired
    private ActiveGameRegistry activeGameRegistry;

    @PostConstruct
    public void init() {
    }

    @RequestMapping("/new-game")
    public BasicResponse createGame(String token, String newGameName, int gameType) {
        if (token == null || newGameName == null || newGameName.trim().isEmpty()) {
            return new BasicResponse(false, ERROR_MISSING_VALUES);
        }

        UserEntity userEntity = persist.getUserByToken(token);
        if (userEntity != null) {
            GameEntity newGame = new GameEntity();
            newGame.setGameName(newGameName);
            newGame.setGameType(gameType);
            newGame.setCreator(userEntity);
            newGame.setStatus(WAITING);
            newGame.setMaxPlayers(MAX_PLAYERS);
            newGame.setTrackLength(1000);
            newGame.setGameCode(generateUniqueGameCode());

            persist.save(newGame);
            ActiveGameState activeGameState = new ActiveGameState();
            activeGameState.setGameId(newGame.getId());
            activeGameState.setRunning(false);
            activeGameState.setFinished(false);
            activeGameState.setTrackLength(1000);
            activeGameState.setMaxPlayers(MAX_PLAYERS);
            activeGameRegistry.addGame(activeGameState);
            return new NewGameResponse(true, null, newGame.getId());
        } else {
            return new BasicResponse(false, ERROR_WRONG_CREDENTIALS);
        }
    }

    @RequestMapping("/get-game")
    public BasicResponse getGame(String token, int id) {
        UserEntity userEntity = persist.getUserByToken(token);
        if (userEntity == null) return new BasicResponse(false, ERROR_WRONG_CREDENTIALS);

        GameEntity game = persist.getGameById(id);
        if (game == null) return new BasicResponse(false, ERROR_MISSING_VALUES);

        ActiveGameState activeGameState = activeGameRegistry.getGame(id);
        List<GamePlayerModel> securePlayers = new java.util.ArrayList<>();

        if (activeGameState != null) {
            game.setStatus(activeGameState.isRunning() ? STARTED : WAITING);
            for (PlayerRuntimeState liveState : activeGameState.getPlayers().values()) {
                securePlayers.add(new GamePlayerModel(liveState));
            }
        } else {
            // שליפה מהד"ב של הנתונים למקרה שרוצים אותם אחרי שהמשחק הסתיים...
            List<GamePlayerEntity> dbPlayers = persist.getGamePlayersByGameId(id);
            for (GamePlayerEntity gp : dbPlayers) {
                securePlayers.add(new GamePlayerModel(gp));
            }
        }

        GameModel secureGameModel = new GameModel(game, securePlayers);
        return new GameResponse(true, null, secureGameModel);
    }

    @RequestMapping("/join-game")
    public BasicResponse joinGame(String token, String gameCode) {
        if (token == null || gameCode == null || gameCode.trim().isEmpty()) {
            return new BasicResponse(false, ERROR_MISSING_VALUES);
        }

        UserEntity userEntity = persist.getUserByToken(token);
        if (userEntity != null) {
            GameEntity game = persist.getGameByGameCode(gameCode,WAITING);

            if (game != null) {

                GamePlayerEntity existingPlayer =
                        persist.getGamePlayerByGameAndUser(game.getId(), userEntity.getId());

                if (existingPlayer != null) {
                    return new NewGameResponse(true, null, game.getId());
                }
                //ההוספה של השחקן לד"ב
                List<UserEntity> players = persist.getPlayersByGameId(game.getId());
                if (players != null && players.size() < MAX_PLAYERS) {
                    GamePlayerEntity gamePlayerEntity = new GamePlayerEntity();
                    gamePlayerEntity.setGame(game);
                    gamePlayerEntity.setPlayer(userEntity);
                    gamePlayerEntity.setScore(0);

                    persist.save(gamePlayerEntity);
                    // ההוספה של השחקן לסטייט
                    ActiveGameState activeGameState = activeGameRegistry.getGame(game.getId());
                    if (activeGameState != null) {
                        PlayerRuntimeState playerState = new PlayerRuntimeState();
                        playerState.setUserId(userEntity.getId());
                        playerState.setUsername(userEntity.getUsername());
                        playerState.setFullName(userEntity.getFullName());
                        playerState.setScore(0);
                        activeGameState.getPlayers().put(userEntity.getId(), playerState);

                        List<GamePlayerModel> allPlayersNow = new java.util.ArrayList<>();
                        for (PlayerRuntimeState state : activeGameState.getPlayers().values()) {
                            allPlayersNow.add(new GamePlayerModel(state));
                        }

                        // שליחת הודעה לכולם על ההצטרפות ועדכון  של הרשימה אצלהם על ידי SSE
                        Map<String, Object> eventData = new java.util.HashMap<>();
                        eventData.put("type", "PLAYERS_LIST_UPDATE");
                        eventData.put("players", allPlayersNow);

                        // עדכון של היוצר
                        if (activeGameState.getCreatorEmitter() != null) {
                            try {
                                activeGameState.getCreatorEmitter().send(
                                        org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                                                .name("gameUpdate")
                                                .data(eventData)
                                );
                            } catch (Exception e) {
                                activeGameState.setCreatorEmitter(null);
                            }
                        }
                        //עדכון של כל השחקנים
                        for (org.springframework.web.servlet.mvc.method.annotation.SseEmitter playerEmitter : activeGameState.getPlayerEmitters()) {
                            try {
                                playerEmitter.send(
                                        org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                                                .name("playersUpdate")
                                                .data(eventData)
                                );
                            } catch (Exception e) {

                            }
                        }
                    }

                    return new NewGameResponse(true, null, game.getId());
                } else {
                    return new BasicResponse(false, ERROR_GAME_IS_FULL);
                }
            } else {
                return new BasicResponse(false, ERROR_MISSING_VALUES);
            }
        } else {
            return new BasicResponse(false, ERROR_WRONG_CREDENTIALS);
        }
    }

    @RequestMapping("/start-game")
    public BasicResponse startGame(String token, int gameId) {
        UserEntity userEntity = persist.getUserByToken(token);
        if (userEntity == null) {
            return new BasicResponse(false, ERROR_WRONG_CREDENTIALS);
        }

        GameEntity game = persist.getGameById(gameId);
        if (game == null) {
            return new BasicResponse(false, ERROR_MISSING_VALUES);
        }

        if (game.getCreator() == null || game.getCreator().getId() != userEntity.getId()) {
            return new BasicResponse(false, ERROR_ONLY_CREATOR_CAN_START_GAME);
        }

        if (game.getStatus() != WAITING) {
            return new BasicResponse(false, ERROR_GAME_ALREADY_STARTED);
        }

        ActiveGameState activeGameState = activeGameRegistry.getGame(gameId);
        if (activeGameState == null) {
            return new BasicResponse(false, ERROR_GAME_NOT_FOUND);
        }

        activeGameState.setRunning(true);

        activeGameState.setStartedAt(System.currentTimeMillis());


        for (org.springframework.web.servlet.mvc.method.annotation.SseEmitter playerEmitter : activeGameState.getPlayerEmitters()) {
            try {
                playerEmitter.send(
                        org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                                .name("statusChange")
                                .data("{\"type\":\"GAME_STARTED\"}")
                );
            } catch (Exception e) {

            }
        }

        game.setStatus(STARTED);
        game.setStartedAt(new Date());
        persist.save(game);

        return new BasicResponse(true, null);
    }

    private String generateUniqueGameCode() {
        for (int i = 0; i < 20; i++) {
            String code = GeneralUtils.generateRandomCode(6);
            if (!persist.doesGameCodeExist(code)) {
                return code;
            }
        }
        throw new RuntimeException("Failed to generate unique game code");
    }
}