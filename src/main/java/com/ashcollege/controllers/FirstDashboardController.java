package com.ashcollege.controllers;

import com.ashcollege.Engine.*;
import com.ashcollege.entities.*;
import com.ashcollege.responses.*;
import com.ashcollege.service.Persist;
import com.ashcollege.utils.GeneralUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.ashcollege.utils.Constants.*;
import static com.ashcollege.utils.Errors.*;

@RestController
public class FirstDashboardController {

    @Autowired
    private Persist persist;

    @Autowired
    private ActiveGameRegistry activeGameRegistry;

    @Autowired
    private SseService sseService;

    @Autowired
    private GameController gameController;

    private synchronized ActiveGameState getOrReviveGame(GameEntity game) {
        ActiveGameState activeGame = activeGameRegistry.getGame(game.getId());
        if (activeGame == null) {
            activeGame = new ActiveGameState();
            activeGame.setGameId(game.getId());
            activeGame.setRunning(game.getStatus() == STARTED);
            activeGame.setFinished(game.getStatus() == FINISHED);
            activeGame.setTrackLength(game.getTrackLength());
            activeGame.setMaxPlayers(game.getMaxPlayers());
            activeGame.setGameType(Math.max(0, Math.min(2, game.getGameType())));

            List<GamePlayerEntity> dbPlayers = persist.getGamePlayersByGameId(game.getId());
            if (dbPlayers != null) {
                for (GamePlayerEntity gp : dbPlayers) {
                    PlayerRuntimeState prs = new PlayerRuntimeState();
                    prs.setUserId(gp.getPlayer().getId());
                    prs.setFullName(gp.getPlayer().getFullName());
                    prs.setUsername(gp.getPlayer().getUsername());
                    prs.setScore(gp.getScore());
                    prs.setCorrectAnswers(gp.getCorrectAnswers());
                    prs.setWrongAnswers(gp.getWrongAnswers());
                    prs.setStreak(gp.getStreak());
                    prs.setFinished(gp.isFinished());
                    activeGame.getPlayers().put(gp.getPlayer().getId(), prs);
                }
            }

            if (game.getStatus() != FINISHED) {
                activeGameRegistry.addGame(activeGame);
            }
        }
        return activeGame;
    }

    @PostMapping("/new-game")
    public BasicResponse createGame(@RequestBody com.ashcollege.requests.NewGameRequest request) {
        if (request.getToken() == null || request.getNewGameName() == null || request.getNewGameName().trim().isEmpty()) {
            return new BasicResponse(false, ERROR_MISSING_VALUES);
        }

        int gameType = request.getGameType();
        if (gameType < 0 || gameType > 2) {
            return new BasicResponse(false, ERROR_MISSING_VALUES);
        }

        UserEntity userEntity = persist.getUserByToken(request.getToken());
        if (userEntity == null) {
            return new BasicResponse(false, ERROR_WRONG_CREDENTIALS);
        }

        try {
            GameEntity newGame = new GameEntity();
            newGame.setGameName(request.getNewGameName().trim());
            newGame.setGameType(gameType);
            newGame.setCreator(userEntity);
            newGame.setStatus(WAITING);
            newGame.setMaxPlayers(MAX_PLAYERS);
            newGame.setTrackLength(TRACK_LENGTH);
            newGame.setGameCode(generateUniqueGameCode());

            persist.save(newGame);

            ActiveGameState activeGameState = new ActiveGameState();
            activeGameState.setGameId(newGame.getId());
            activeGameState.setRunning(false);
            activeGameState.setFinished(false);
            activeGameState.setTrackLength(TRACK_LENGTH);
            activeGameState.setMaxPlayers(MAX_PLAYERS);
            activeGameState.setGameType(gameType);
            activeGameRegistry.addGame(activeGameState);

            return new NewGameResponse(true, null, newGame.getId());

        } catch (Exception e) {
            e.printStackTrace();
            return new BasicResponse(false, ERROR_MISSING_VALUES);
        }
    }

    @GetMapping("/get-game")
    public BasicResponse getGame(String token, int id) {
        UserEntity userEntity = persist.getUserByToken(token);
        if (userEntity == null) return new BasicResponse(false, ERROR_WRONG_CREDENTIALS);

        GameEntity game = persist.getGameById(id);
        if (game == null) return new BasicResponse(false, ERROR_MISSING_VALUES);

        ActiveGameState activeGameState = getOrReviveGame(game);

        List<GamePlayerModel> securePlayers = new java.util.ArrayList<>();

        for (PlayerRuntimeState prs : activeGameState.getPlayers().values()) {
            securePlayers.add(new GamePlayerModel(prs.getUserId(), prs.getFullName(), prs));
        }

        GameModel secureGameModel = new GameModel(game, securePlayers);

        if (activeGameState.isFinished()) {
            secureGameModel.setStatus(FINISHED);
        } else if (activeGameState.isRunning()) {
            secureGameModel.setStatus(STARTED);
        } else {
            secureGameModel.setStatus(WAITING);
        }

        return new GameResponse(true, null, secureGameModel);
    }

    @PostMapping("/join-game")
    public BasicResponse joinGame(@RequestBody com.ashcollege.requests.JoinGameRequest request) {
        if (request.getToken() == null || request.getGameCode() == null || request.getGameCode().trim().isEmpty()) {
            return new BasicResponse(false, ERROR_MISSING_VALUES);
        }

        try {
            UserEntity user = persist.getUserByToken(request.getToken());
            if (user == null) return new BasicResponse(false, ERROR_WRONG_CREDENTIALS);

            String code = request.getGameCode().trim();

            GameEntity game = persist.getGameByGameCode(code, WAITING);

            if (game == null) {
                GameEntity startedGame = persist.getGameByGameCode(code, STARTED);
                if (startedGame != null) {
                    GamePlayerEntity existingInStarted = persist.getGamePlayerByGameAndUser(startedGame.getId(), user.getId());
                    if (existingInStarted != null) {
                        return new NewGameResponse(true, null, startedGame.getId());
                    }
                    return new BasicResponse(false, ERROR_GAME_ALREADY_STARTED);
                }

                GameEntity finishedGame = persist.getGameByGameCode(code, FINISHED);
                if (finishedGame != null) {
                    return new BasicResponse(false, ERROR_GAME_FINISHED);
                }

                return new BasicResponse(false, ERROR_GAME_NOT_FOUND);
            }

            ActiveGameState activeGame = getOrReviveGame(game);

            boolean playerAdded = false;
            List<GamePlayerModel> livePlayers;

            synchronized (activeGame.getLock()) {
                GamePlayerEntity existingPlayer = persist.getGamePlayerByGameAndUser(game.getId(), user.getId());

                if (existingPlayer == null && !activeGame.getPlayers().containsKey(user.getId())) {
                    List<GamePlayerEntity> currentPlayers = persist.getGamePlayersByGameId(game.getId());
                    if (currentPlayers != null && currentPlayers.size() >= MAX_PLAYERS) {
                        return new BasicResponse(false, ERROR_GAME_IS_FULL);
                    }

                    GamePlayerEntity newPlayer = new GamePlayerEntity();
                    newPlayer.setGame(game);
                    newPlayer.setPlayer(user);
                    newPlayer.setScore(0);
                    newPlayer.setCorrectAnswers(0);
                    newPlayer.setWrongAnswers(0);
                    newPlayer.setStreak(0);
                    newPlayer.setFinished(false);

                    persist.save(newPlayer);
                    persist.flush();

                    PlayerRuntimeState playerState = new PlayerRuntimeState();
                    playerState.setUserId(user.getId());
                    playerState.setFullName(user.getFullName());
                    playerState.setUsername(user.getUsername());
                    activeGame.getPlayers().put(user.getId(), playerState);

                    playerAdded = true;
                }

                livePlayers = new java.util.ArrayList<>();
                for (PlayerRuntimeState prs : activeGame.getPlayers().values()) {
                    livePlayers.add(new GamePlayerModel(prs.getUserId(), prs.getFullName(), prs));
                }
            }

            if (playerAdded) {
                Map<String, Object> joinEventData = new java.util.HashMap<>();
                joinEventData.put("type", "PLAYERS_LIST_UPDATE");
                joinEventData.put("players", livePlayers);
                sseService.broadcastToGame(game.getId(), "gameEvent", joinEventData);

                if (livePlayers.size() >= MAX_PLAYERS) {
                    Map<String, Object> fullEventData = new java.util.HashMap<>();
                    fullEventData.put("type", "GAME_FULL");
                    fullEventData.put("playerCount", livePlayers.size());
                    sseService.broadcastToGame(game.getId(), "gameEvent", fullEventData);
                }
            }

            return new NewGameResponse(true, null, game.getId());

        } catch (Exception e) {
            e.printStackTrace();
            return new BasicResponse(false, ERROR_MISSING_VALUES);
        }
    }

    @PostMapping("/start-game")
    public BasicResponse startGame(@RequestBody com.ashcollege.requests.StartGameRequest request) {
        UserEntity userEntity = persist.getUserByToken(request.getToken());
        if (userEntity == null) return new BasicResponse(false, ERROR_WRONG_CREDENTIALS);

        GameEntity game = persist.getGameById(request.getGameId());
        if (game == null) return new BasicResponse(false, ERROR_MISSING_VALUES);

        if (game.getCreator() == null || game.getCreator().getId() != userEntity.getId()) {
            return new BasicResponse(false, ERROR_ONLY_CREATOR_CAN_START_GAME);
        }

        if (game.getStatus() != WAITING) {
            return new BasicResponse(false, ERROR_GAME_ALREADY_STARTED);
        }

        List<GamePlayerEntity> players = persist.getGamePlayersByGameId(game.getId());
        if (players == null || players.size() < MIN_PLAYERS) {
            return new BasicResponse(false, ERROR_NOT_ENOUGH_PLAYERS);
        }

        ActiveGameState activeGameState = getOrReviveGame(game);
        activeGameState.setRunning(true);
        activeGameState.setStartedAt(System.currentTimeMillis());

        Map<String, Object> startEventData = new java.util.HashMap<>();
        startEventData.put("type", "GAME_STARTED");
        startEventData.put("startedAt", activeGameState.getStartedAt());
        sseService.broadcastToGame(request.getGameId(), "gameEvent", startEventData);

        game.setStatus(STARTED);
        game.setStartedAt(new Date());
        persist.save(game);

        return new BasicResponse(true, null);
    }

    @PostMapping("/end-game")
    public BasicResponse endGame(@RequestBody com.ashcollege.requests.StartGameRequest request) {
        UserEntity userEntity = persist.getUserByToken(request.getToken());
        if (userEntity == null) return new BasicResponse(false, ERROR_WRONG_CREDENTIALS);

        GameEntity game = persist.getGameById(request.getGameId());
        if (game == null) return new BasicResponse(false, ERROR_MISSING_VALUES);

        if (game.getCreator() == null || game.getCreator().getId() != userEntity.getId()) {
            return new BasicResponse(false, ERROR_ONLY_CREATOR_CAN_END_GAME);
        }

        ActiveGameState activeGameState = activeGameRegistry.getGame(request.getGameId());
        if (activeGameState == null || !activeGameState.isRunning()) {
            return new BasicResponse(false, ERROR_GAME_NOT_ACTIVE);
        }

        gameController.endGameManually(request.getGameId(), activeGameState);

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