package com.ashcollege.Engine;



import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ActiveGameRegistry {

    private final Map<Integer, ActiveGameState> activeGames = new ConcurrentHashMap<>();

    public void addGame(ActiveGameState gameState) {
        activeGames.put(gameState.getGameId(), gameState);
    }

    public ActiveGameState getGame(int gameId) {
        return activeGames.get(gameId);
    }

    public void removeGame(int gameId) {
        activeGames.remove(gameId);
    }

    public Collection<ActiveGameState> getAllGames() {
        return activeGames.values();
    }

    public boolean containsGame(int gameId) {
        return activeGames.containsKey(gameId);
    }
}
