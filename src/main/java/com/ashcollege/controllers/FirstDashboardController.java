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
import static com.ashcollege.utils.Errors.ERROR_MISSING_VALUES;

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
            return new NewGameResponse(true, null, newGame.getId());
        } else {
            return new BasicResponse(false, ERROR_WRONG_CREDENTIALS);
        }
    }

    @RequestMapping("/get-game")
    public BasicResponse getGame(String token, int id) {
        UserEntity userEntity = persist.getUserByToken(token);
        if (userEntity != null) {
            GameEntity game = persist.getGameById(id);
            if (game == null) {
                return new BasicResponse(false, ERROR_MISSING_VALUES);
            }

            List<UserEntity> players = persist.getPlayersByGameId(id);

            boolean isCreator = game.getCreator() != null && game.getCreator().getId() == userEntity.getId();

            boolean isPlayer = players.stream().anyMatch(p -> p.getId() == userEntity.getId());

            if (!isCreator && !isPlayer) {
                return new BasicResponse(false, ERROR_NO_PREMITION);
            }

            return new GameResponse(true, null, game, players);
        } else {
            return new BasicResponse(false, ERROR_WRONG_CREDENTIALS);
        }
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

                List<UserEntity> players = persist.getPlayersByGameId(game.getId());
                if (players != null && players.size() < MAX_PLAYERS) {
                    GamePlayerEntity gamePlayerEntity = new GamePlayerEntity();
                    gamePlayerEntity.setGame(game);
                    gamePlayerEntity.setPlayer(userEntity);
                    gamePlayerEntity.setScore(0);

                    persist.save(gamePlayerEntity);
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
        if (activeGameRegistry.containsGame(gameId)) {
            return new BasicResponse(false, ERROR_GAME_ALREADY_STARTED);
        }
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

        List<GamePlayerEntity> gamePlayers = persist.getGamePlayersByGameId(gameId);
        if (gamePlayers == null || gamePlayers.size() < MIN_PLAYERS) {
            return new BasicResponse(false, ERROR_NOT_ENOUGH_PLAYERS);
        }

        ActiveGameState activeGameState = new ActiveGameState();
        activeGameState.setGameId(game.getId());
        activeGameState.setRunning(true);
        activeGameState.setFinished(false);
        activeGameState.setStartedAt(System.currentTimeMillis());
        activeGameState.setLastTickTime(System.currentTimeMillis());
        activeGameState.setTrackLength(TRACK_LENGTH);
        activeGameState.setMaxPlayers(MAX_PLAYERS);

        for (GamePlayerEntity gp : gamePlayers) {

            UserEntity player = gp.getPlayer();

            PlayerRuntimeState playerState = new PlayerRuntimeState();
            playerState.setUserId(player.getId());
            playerState.setUsername(player.getUsername());
            playerState.setFullName(player.getFullName());
            playerState.setScore(0);
            playerState.setCorrectAnswers(0);
            playerState.setWrongAnswers(0);
            playerState.setStreak(0);
            playerState.setFinished(false);


            activeGameState.getPlayers().put(player.getId(), playerState);
        }

        activeGameRegistry.addGame(activeGameState);

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