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
public class AuthController {
    @Autowired
    private Persist persist;

    @PostConstruct
    public void init() {
    }

    @RequestMapping("/login")
    public BasicResponse getUser (String username, String password) {
        try {
            if (username != null && password != null) {
                UserEntity userEntity = persist.getUserByUsernameAndPassword(username, password);
                if (userEntity != null) {
                    String token = GeneralUtils.hashMd5(username, password);
                    userEntity.setToken(token);
                    persist.save(userEntity);
                    return new LoginResponse(true, null, 1, token, userEntity.getId());
                } else {
                    return new BasicResponse(false,  ERROR_WRONG_CREDENTIALS);
                }
            } else {
                return new BasicResponse(false, ERROR_MISSING_USERNAME_OR_PASSWORD);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    @RequestMapping("/signup")
    public BasicResponse addUser(String username,String password,String fullName) {
        try {
            if (username != null  && password != null && fullName != null ) {
                UserEntity userEntity = persist.getUserByUsername(username);
                if (userEntity != null) {
                    return new BasicResponse(false,ERROR_USERNAME_ALREADY_EXISTS);
                }else {
                    UserEntity user = new UserEntity();
                    user.setUsername(username);
                    user.setPassword(password);
                    user.setFullName(fullName);
                    String token = GeneralUtils.hashMd5(username, password);
                    user.setToken(token);
                    persist.save(user);
                    return new LoginResponse(true, null, 1, token, user.getId());
                }
            }else {
                return new BasicResponse(false, ERROR_MISSING_VALUES);
            }

        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    @RequestMapping("/get-default-params")
    public BasicResponse getDefaultParams (String token) {
        UserEntity userEntity = persist.getUserByToken(token);
        if (userEntity != null) {
            return new DefaultParamResponse(true, null, userEntity);
        } else {
            return new BasicResponse(false, ERROR_WRONG_CREDENTIALS);
        }
    }










}