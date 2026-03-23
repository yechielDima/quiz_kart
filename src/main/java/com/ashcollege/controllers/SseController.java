package com.ashcollege.controllers;


import com.ashcollege.Engine.ActiveGameRegistry;
import com.ashcollege.entities.UserEntity;
import com.ashcollege.service.Persist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class SseController {

    @Autowired
    private ActiveGameRegistry activeGameRegistry;

    @Autowired
    private Persist persist;

    @Autowired
    private SseService sseService;

    @RequestMapping("/game-subscribe")
    public SseEmitter subscribe(String token, int gameId) {
        UserEntity user = persist.getUserByToken(token);
        if (user == null) {
            return null;
        }

        // מוודאים שהמשחק אכן קיים ופעיל בזיכרון
        if (!activeGameRegistry.containsGame(gameId)) {
            return null;
        }

        // המרכזייה דואגת ליצירת ה-Emitter, ניהול הניתוקים ופעימות הלב
        return sseService.subscribe(gameId, user.getId());
    }
}