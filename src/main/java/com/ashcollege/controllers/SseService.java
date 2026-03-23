package com.ashcollege.controllers;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class SseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SseService.class);

    // מיפוי: GameId -> (UserId -> SseEmitter)
    // ככה אנחנו מפרידים בין החדרים השונים ומוצאים משתמש ספציפי בשליפה מיידית
    private final Map<Integer, Map<Integer, SseEmitter>> gameEmitters = new ConcurrentHashMap<>();

    // יצירת תהליכון ברקע שישדר את ההודעות בלי לתקוע את השרת
    private final ExecutorService cachedThreadPool = Executors.newCachedThreadPool();

    /**
     * הרשמה של משתמש לקבלת עדכונים
     */
    public SseEmitter subscribe(int gameId, int userId) {
        // טיימאאוט של חצי שעה, אבל אנחנו נשמור עליו חי עם פעימות לב
        SseEmitter emitter = new SseEmitter(30L * 60L * 1000L);

        gameEmitters.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>()).put(userId, emitter);

        // ניקוי אוטומטי במקרה של ניתוק
        emitter.onCompletion(() -> removeEmitter(gameId, userId));
        emitter.onTimeout(() -> removeEmitter(gameId, userId));
        emitter.onError((e) -> removeEmitter(gameId, userId));

        return emitter;
    }

    /**
     * שליחת אירוע לחדר שלם - באופן אסינכרוני!
     */
    public void broadcastToGame(int gameId, String eventName, Object data) {
        Map<Integer, SseEmitter> roomEmitters = gameEmitters.get(gameId);
        if (roomEmitters != null) {
            for (Map.Entry<Integer, SseEmitter> entry : roomEmitters.entrySet()) {
                int userId = entry.getKey();
                SseEmitter emitter = entry.getValue();

                // שליחה בתהליכון נפרד כדי לא לעכב את הלופ
                cachedThreadPool.execute(() -> {
                    try {
                        emitter.send(SseEmitter.event().name(eventName).data(data));
                    } catch (IOException | IllegalStateException e) {
                        LOGGER.warn("Failed to send event to user {} in game {}. Removing connection.", userId, gameId);
                        removeEmitter(gameId, userId);
                    }
                });
            }
        }
    }

    /**
     * שליחת הודעה למשתמש ספציפי (למשל: "קיבלת צומת!")
     */
    public void sendToUser(int gameId, int userId, String eventName, Object data) {
        Map<Integer, SseEmitter> roomEmitters = gameEmitters.get(gameId);
        if (roomEmitters != null && roomEmitters.containsKey(userId)) {
            cachedThreadPool.execute(() -> {
                try {
                    roomEmitters.get(userId).send(SseEmitter.event().name(eventName).data(data));
                } catch (IOException | IllegalStateException e) {
                    removeEmitter(gameId, userId);
                }
            });
        }
    }

    private void removeEmitter(int gameId, int userId) {
        Map<Integer, SseEmitter> roomEmitters = gameEmitters.get(gameId);
        if (roomEmitters != null) {
            roomEmitters.remove(userId);
            if (roomEmitters.isEmpty()) {
                gameEmitters.remove(gameId);
            }
        }
    }

    /**
     * מנגנון "פעימות לב" - חובה למערכות פרודקשן
     * רץ כל 15 שניות ושולח פינג שקוף כדי למנוע מהדפדפן לסגור את החיבור
     */
    @Scheduled(fixedRate = 15000)
    public void sendHeartbeat() {
        gameEmitters.forEach((gameId, roomEmitters) -> {
            roomEmitters.forEach((userId, emitter) -> {
                cachedThreadPool.execute(() -> {
                    try {
                        emitter.send(SseEmitter.event().name("ping").data("keep-alive"));
                    } catch (IOException | IllegalStateException e) {
                        removeEmitter(gameId, userId);
                    }
                });
            });
        });
    }
}