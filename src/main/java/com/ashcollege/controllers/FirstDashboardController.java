package com.ashcollege.controllers;

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

import java.util.List;

import static com.ashcollege.utils.Constants.USER_TYPE_CLIENT;
import static com.ashcollege.utils.Errors.*;
import static com.ashcollege.utils.Errors.ERROR_MISSING_VALUES;

@RestController
public class FirstDashboardController {
    @Autowired
    private Persist persist;

    @PostConstruct
    public void init() {
    }

    @RequestMapping("/new-game")
    public BasicResponse getUser(String token, String newGameName, int gameType) {
            UserEntity userEntity = persist.getUserByToken(token);
            if (userEntity != null) {

                    GameEntity newGame = new GameEntity();

                    newGame.setGameName(newGameName);
                    newGame.setGameType(gameType);

                    newGame.setCreator(userEntity);

                    persist.save(newGame);
                    return new NewGameResponse(true,null,newGame.getId());

            } else {
                return new BasicResponse(false, ERROR_WRONG_CREDENTIALS);
            }

    }

    @RequestMapping("/get-game")
    public BasicResponse getGame(String token, int id) {
        UserEntity userEntity = persist.getUserByToken(token);
        if (userEntity != null) {

            GameEntity game = persist.getGameById(id);
            List<UserEntity> players = persist.getPlayersByGameId(id);


            return new GameResponse(true,null,game,players);

        } else {
            return new BasicResponse(false, ERROR_WRONG_CREDENTIALS);
        }

    }
    @RequestMapping("/join-game")
    public BasicResponse joinGame(String token,String gameCode) {
        UserEntity userEntity = persist.getUserByToken(token);
        if (userEntity != null) {
            GameEntity game = persist.getGameByGameCode(gameCode);
            if (game != null && game.getStatus() == 0) {
                List<UserEntity> players = persist.getPlayersByGameId(game.getId());
                if (players != null && players.size() < 8) {
                    GamePlayerEntity gamePlayerEntity = new GamePlayerEntity();
                    gamePlayerEntity.setGame(game);
                    gamePlayerEntity.setPlayer(userEntity);
                    gamePlayerEntity.setScore(0);
                    persist.save(gamePlayerEntity);
                    return new NewGameResponse(true,null,game.getId());
                }else  {
                    return new BasicResponse(false, ERROR_GAME_IS_FULL);
                }
            }else  {
                return new BasicResponse(false, ERROR_MISSING_VALUES);
            }
        } else {
            return new BasicResponse(false, ERROR_WRONG_CREDENTIALS);
        }

    }



}