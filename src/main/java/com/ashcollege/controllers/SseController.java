package com.ashcollege.controllers;
import com.ashcollege.Engine.ActiveGameRegistry;
import com.ashcollege.Engine.ActiveGameState;
import com.ashcollege.entities.GameEntity;
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

    @RequestMapping("/game-subscribe")
    public SseEmitter subscribe(String token, int gameId) {
        UserEntity user = persist.getUserByToken(token);
        if (user == null) {
            return null;
        }

        ActiveGameState gameState = activeGameRegistry.getGame(gameId);
        if (gameState == null) {
            return null;
        }

        GameEntity game = persist.getGameById(gameId);

        SseEmitter emitter = new SseEmitter(60L * 60L * 1000L);

        if (game.getCreator().getId() == user.getId()) {
            gameState.setCreatorEmitter(emitter);
        } else {
            gameState.addPlayerEmitter(emitter);
        }

        emitter.onCompletion(() -> removeEmitter(gameState, game, user, emitter));
        emitter.onTimeout(() -> removeEmitter(gameState, game, user, emitter));
        emitter.onError((e) -> removeEmitter(gameState, game, user, emitter));

        return emitter;
    }

    private void removeEmitter(ActiveGameState gameState, GameEntity game, UserEntity user, SseEmitter emitter) {
        if (game.getCreator().getId() == user.getId()) {
            gameState.setCreatorEmitter(null);
        } else {
            gameState.removePlayerEmitter(emitter);
        }
    }
}