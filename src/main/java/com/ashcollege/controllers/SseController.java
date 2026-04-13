package com.ashcollege.controllers;

import com.ashcollege.Engine.ActiveGameRegistry;
import com.ashcollege.entities.UserEntity;
import com.ashcollege.service.Persist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("/game-subscribe")
    public SseEmitter subscribe(String token, int gameId) {
        if (token == null || token.trim().isEmpty()) {
            SseEmitter emitter = new SseEmitter(0L);
            emitter.completeWithError(new RuntimeException("Missing token"));
            return emitter;
        }

        UserEntity user = persist.getUserByToken(token);
        if (user == null) {
            SseEmitter emitter = new SseEmitter(0L);
            emitter.completeWithError(new RuntimeException("Invalid token"));
            return emitter;
        }

        if (!activeGameRegistry.containsGame(gameId)) {
            SseEmitter emitter = new SseEmitter(0L);
            emitter.completeWithError(new RuntimeException("Game not found"));
            return emitter;
        }

        return sseService.subscribe(gameId, user.getId());
    }
}