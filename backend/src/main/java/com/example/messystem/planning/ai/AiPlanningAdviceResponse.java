package com.example.messystem.planning.ai;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class AiPlanningAdviceResponse {
    public boolean enabled;
    public String model;
    public LocalDateTime generatedAt;
    public int inputTaskCount;
    public Map<String, Object> advice;
    public List<String> validationWarnings;
}
