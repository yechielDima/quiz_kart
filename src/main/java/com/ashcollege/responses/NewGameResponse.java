package com.ashcollege.responses;

import com.ashcollege.entities.UserEntity;

public class NewGameResponse extends BasicResponse{
    private int id;
    public NewGameResponse (boolean success, Integer errorCode, int id) {

        super(success, errorCode);
        this.id = id;
    }
    public int getId() {return id;}
    public void setId(int id) {this.id = id;}
}
