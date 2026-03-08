package com.ashcollege.responses;

import com.ashcollege.entities.UserEntity;

public class UserModel {
    private String fullName;
    private int id;
    public UserModel(UserEntity user) {
        this.id = user.getId();
        this.fullName = user.getFullName();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}
