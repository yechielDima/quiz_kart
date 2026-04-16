package com.ashcollege.Engine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ActiveGameState {

    private int gameId;
    private volatile boolean running;
    private volatile boolean finished;
    private volatile long startedAt;
    private long lastTickTime;
    private int trackLength;
    private int maxPlayers;
    private final Object lock = new Object();
    private Map<Integer, PlayerRuntimeState> players = new ConcurrentHashMap<>();

    public int getGameId() {
        return gameId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(long startedAt) {
        this.startedAt = startedAt;
    }

    public long getLastTickTime() {
        return lastTickTime;
    }

    public void setLastTickTime(long lastTickTime) {
        this.lastTickTime = lastTickTime;
    }

    public int getTrackLength() {
        return trackLength;
    }

    public void setTrackLength(int trackLength) {
        this.trackLength = trackLength;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public Object getLock() {
        return lock;
    }

    public Map<Integer, PlayerRuntimeState> getPlayers() {
        return players;
    }

    public void setPlayers(Map<Integer, PlayerRuntimeState> players) {
        this.players = players;
    }
}