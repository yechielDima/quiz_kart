package com.ashcollege.responses;

import java.util.List;
import java.util.Map;

public class StatsResponse extends BasicResponse {
    private Map<String, Object> stats;

    public StatsResponse(boolean success, Integer errorCode, Map<String, Object> stats) {
        super(success, errorCode);
        this.stats = stats;
    }

    public Map<String, Object> getStats() {
        return stats;
    }

    public void setStats(Map<String, Object> stats) {
        this.stats = stats;
    }
}
