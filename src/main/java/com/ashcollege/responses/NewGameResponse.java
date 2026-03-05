package com.ashcollege.responses;

import com.ashcollege.entities.UserEntity;

public class NewGameResponse extends BasicResponse{
    public NewGameResponse (boolean success, Integer errorCode, String gameCode) {
        super(success, errorCode);
    }
}
