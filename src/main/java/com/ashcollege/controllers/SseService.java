package com.ashcollege.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class SseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SseService.class);

    private final Map<Integer, Map<Integer, SseEmitter>> gameEmitters = new ConcurrentHashMap<>();
    private final ExecutorService threadPool = Executors.newFixedThreadPool(50);

    public SseEmitter subscribe(int gameId, int userId) {
        SseEmitter emitter = new SseEmitter(30L * 60L * 1000L);

        gameEmitters.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>()).put(userId, emitter);

        emitter.onCompletion(() -> removeEmitter(gameId, userId));
        emitter.onTimeout(() -> removeEmitter(gameId, userId));
        emitter.onError((e) -> removeEmitter(gameId, userId));

        return emitter;
    }

    public void broadcastToGame(int gameId, String eventName, Object data) {
        Map<Integer, SseEmitter> roomEmitters = gameEmitters.get(gameId);
        if (roomEmitters != null) {
            for (Map.Entry<Integer, SseEmitter> entry : roomEmitters.entrySet()) {
                int userId = entry.getKey();
                SseEmitter emitter = entry.getValue();

                threadPool.execute(() -> {
                    try {
                        emitter.send(SseEmitter.event().name(eventName).data(data));
                    } catch (IOException | IllegalStateException e) {
                        LOGGER.warn("Failed to send to user {} in game {}.", userId, gameId);
                        removeEmitter(gameId, userId);
                    }
                });
            }
        }
    }

    public void sendToUser(int gameId, int userId, String eventName, Object data) {
        Map<Integer, SseEmitter> roomEmitters = gameEmitters.get(gameId);
        if (roomEmitters == null) return;

        SseEmitter emitter = roomEmitters.get(userId);
        if (emitter == null) return;

        threadPool.execute(() -> {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException | IllegalStateException e) {
                removeEmitter(gameId, userId);
            }
        });
    }

    private void removeEmitter(int gameId, int userId) {
        Map<Integer, SseEmitter> roomEmitters = gameEmitters.get(gameId);
        if (roomEmitters != null) {
            roomEmitters.remove(userId);
            if (roomEmitters.isEmpty()) {
                gameEmitters.remove(gameId, roomEmitters);
            }
        }
    }

    @Scheduled(fixedRate = 15000)
    public void sendHeartbeat() {
        gameEmitters.forEach((gameId, roomEmitters) -> {
            roomEmitters.forEach((userId, emitter) -> {
                threadPool.execute(() -> {
                    try {
                        emitter.send(SseEmitter.event().name("ping").data("keep-alive"));
                    } catch (IOException | IllegalStateException e) {
                        removeEmitter(gameId, userId);
                    }
                });
            });
        });
    }

    @PreDestroy
    public void shutdown() {
        LOGGER.info("Shutting down SSE thread pool...");
        threadPool.shutdown();
    }
}