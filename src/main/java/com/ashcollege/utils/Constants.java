package com.ashcollege.utils;

public class Constants {
    public static final String SCHEMA = "quiz_kart";
    public static final String DB_HOST = "localhost";
    public static final int DB_PORT = 3306;
    public static final String DB_USERNAME = "root";
    public static final String DB_PASSWORD = "1234";

    public static final int WAITING = 0;
    public static final int STARTED = 1;
    public static final int FINISHED = 2;

    public static final int MAX_PLAYERS = 8;
    public static final int MIN_PLAYERS = 1;

    public static final int TRACK_LENGTH = 1000;

    public static final int QUESTION_EASY = 0;
    public static final int QUESTION_NORMAL = 1;
    public static final int QUESTION_HARD = 2;

    public static final int QUESTION_TIME_EASY = 15;
    public static final int QUESTION_TIME_NORMAL = 15;
    public static final int QUESTION_TIME_HARD = 10;

    public static final int ANSWER_GRACE_MS = 2000;

    public static final int JUNCTION_NONE = 0;
    public static final int JUNCTION_AUTOSTRADA = 1;
    public static final int JUNCTION_DIRT_ROAD = 2;

    public static final int DECISION_METER_MIN = 4;
    public static final int DECISION_METER_MAX = 8;

    public static final int NORMAL_BASE_POINTS = 20;
    public static final int NORMAL_STREAK_BONUS = 2;
    public static final int NORMAL_MAX_STREAK_BONUS = 10;
    public static final int NORMAL_FAST_TIME_BONUS = 5;
    public static final int NORMAL_MEDIUM_TIME_BONUS = 3;

    public static final int AUTOSTRADA_REWARD = 200;
    public static final int AUTOSTRADA_PENALTY = 50;

    public static final int DIRT_ROAD_QUESTIONS = 5;
    public static final int DIRT_ROAD_POINTS = 10;

    public static final int LUCK_METER_THRESHOLD = 100;
    public static final int LUCK_METER_INCREMENT_MIN = 15;
    public static final int LUCK_METER_INCREMENT_MAX = 30;

    public static final String LUCK_TURBO = "TURBO";
    public static final String LUCK_DOUBLE_POINTS = "DOUBLE_POINTS";
    public static final String LUCK_FLAT_TIRE = "FLAT_TIRE";
    public static final String LUCK_OIL_SLICK = "OIL_SLICK";

    public static final int TURBO_BONUS = 30;
    public static final int OIL_SLICK_PENALTY = 15;

    public static final String EFFECT_NONE = "none";
    public static final String EFFECT_DOUBLE_POINTS = "double_points";
    public static final String EFFECT_FLAT_TIRE = "flat_tire";
}