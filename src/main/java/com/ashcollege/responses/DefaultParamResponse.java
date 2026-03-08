package com.ashcollege.responses;

import com.ashcollege.entities.UserEntity;



public class DefaultParamResponse extends BasicResponse{
    private int id;

    public DefaultParamResponse (boolean success, Integer errorCode, UserEntity userEntity) {
        super(success, errorCode);
        this.id = userEntity.getId();
    }

}
