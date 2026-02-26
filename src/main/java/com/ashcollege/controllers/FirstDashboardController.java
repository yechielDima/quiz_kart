package com.ashcollege.controllers;

import com.ashcollege.entities.UserEntity;
import com.ashcollege.responses.BasicResponse;
import com.ashcollege.responses.DefaultParamResponse;
import com.ashcollege.responses.LoginResponse;
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
    public BasicResponse getUser(String token, String gameName, int type) {
            UserEntity userEntity = persist.getUserByToken(token);
            if (userEntity != null) {
                return new BasicResponse(true, ERROR_NOT_AUTHORIZED);/*צריך פה להחליף לפעולה של פתיחת משחק חדש*/
            } else {
                return new BasicResponse(false, ERROR_WRONG_CREDENTIALS);
            }

    }



}