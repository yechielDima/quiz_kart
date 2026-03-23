package com.ashcollege.responses;

import com.ashcollege.entities.UserEntity;



public class DefaultParamResponse extends BasicResponse{
    private int id;
    private String fullName;

    public DefaultParamResponse (boolean success, Integer errorCode, UserEntity userEntity) {
        super(success, errorCode);
        this.id = userEntity.getId();
        this.fullName = userEntity.getFullName();
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
