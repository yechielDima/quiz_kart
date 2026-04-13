package com.ashcollege.controllers;

import com.ashcollege.entities.UserEntity;
import com.ashcollege.responses.BasicResponse;
import com.ashcollege.responses.DefaultParamResponse;
import com.ashcollege.responses.LoginResponse;
import com.ashcollege.service.Persist;
import com.ashcollege.utils.GeneralUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static com.ashcollege.utils.Errors.*;

@RestController
public class AuthController {

    @Autowired
    private Persist persist;

    @PostMapping("/login")
    public BasicResponse getUser(@RequestBody com.ashcollege.requests.LoginRequest request) {
        try {
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()
                    || request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                return new BasicResponse(false, ERROR_MISSING_VALUES);
            }

            String username = request.getUsername().trim();
            String hashedPass = GeneralUtils.hashPassword(username, request.getPassword());
            UserEntity userEntity = persist.getUserByUsernameAndPassword(username, hashedPass);

            if (userEntity != null) {
                String token = GeneralUtils.hashPassword(username, request.getPassword() + System.currentTimeMillis());
                userEntity.setToken(token);
                persist.save(userEntity);
                return new LoginResponse(true, null, token, userEntity.getId());
            } else {
                return new BasicResponse(false, ERROR_WRONG_CREDENTIALS);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new BasicResponse(false, ERROR_WRONG_CREDENTIALS);
        }
    }

    @PostMapping("/signup")
    public BasicResponse addUser(@RequestBody com.ashcollege.requests.SignupRequest request) {
        try {
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()
                    || request.getPassword() == null || request.getPassword().length() != 6
                    || request.getFullName() == null || request.getFullName().trim().isEmpty()) {
                return new BasicResponse(false, ERROR_MISSING_VALUES);
            }

            String username = request.getUsername().trim();
            String fullName = request.getFullName().trim();

            UserEntity userEntity = persist.getUserByUsername(username);
            if (userEntity != null) {
                return new BasicResponse(false, ERROR_USERNAME_ALREADY_EXISTS);
            }

            UserEntity user = new UserEntity();
            user.setUsername(username);
            user.setPassword(GeneralUtils.hashPassword(username, request.getPassword()));
            user.setFullName(fullName);

            String token = GeneralUtils.hashPassword(username, request.getPassword() + System.currentTimeMillis());
            user.setToken(token);
            persist.save(user);
            return new LoginResponse(true, null, token, user.getId());

        } catch (Exception e) {
            e.printStackTrace();
            return new BasicResponse(false, ERROR_MISSING_VALUES);
        }
    }

    @GetMapping("/get-default-params")
    public BasicResponse getDefaultParams(String token) {
        if (token == null || token.trim().isEmpty()) {
            return new BasicResponse(false, ERROR_MISSING_VALUES);
        }
        UserEntity userEntity = persist.getUserByToken(token);
        if (userEntity != null) {
            return new DefaultParamResponse(true, null, userEntity);
        } else {
            return new BasicResponse(false, ERROR_WRONG_CREDENTIALS);
        }
    }
}
