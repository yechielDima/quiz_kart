package com.ashcollege.controllers;

import com.ashcollege.entities.*;
import com.ashcollege.responses.*;
import com.ashcollege.service.Persist;
import com.ashcollege.utils.GeneralUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.annotation.PostConstruct;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.ashcollege.utils.Constants.USER_TYPE_CLIENT;
import static com.ashcollege.utils.Errors.*;

@RestController
public class GeneralController {
    @Autowired
    private Persist persist;

    @PostConstruct
    public void init() {
    }







}
