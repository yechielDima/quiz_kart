package com.ashcollege.controllers;

import com.ashcollege.entities.GameEntity;
import com.ashcollege.entities.UserEntity;
import com.ashcollege.responses.BasicResponse;
import com.ashcollege.responses.DefaultParamResponse;
import com.ashcollege.responses.LoginResponse;
import com.ashcollege.responses.NewGameResponse;
import com.ashcollege.service.Persist;
import com.ashcollege.utils.GeneralUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;

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

    @RequestMapping("/newGame")
    public BasicResponse getUser(String token, String newGameName, int gameType) {
            UserEntity userEntity = persist.getUserByToken(token);
            if (userEntity != null) {

                    GameEntity newGame = new GameEntity();

                    newGame.setGameName(newGameName);
                    newGame.setGameType(gameType);

                    newGame.setCreator(userEntity);

                    persist.save(newGame);
                    return new NewGameResponse(true,null,newGame.getGameCode());

            } else {
                return new BasicResponse(false, ERROR_WRONG_CREDENTIALS);
            }

    }



}