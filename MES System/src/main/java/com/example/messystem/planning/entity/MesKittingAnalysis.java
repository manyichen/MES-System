package com.example.messystem.planning.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MesKittingAnalysis {
    public Long analysisId;
    public String analysisNo;
    public Long taskId;
    public Long productId;
    public Integer planQty;
    public String kittingStatus;
    public String analysisResult;
    public LocalDateTime analysisTime;
    public List<MesKittingShortageItem> shortageItems = new ArrayList<>();
}
