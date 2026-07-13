package com.example.messystem.trace.entity;

import java.util.List;

public record TireGenerationResult(
        int requestedQuantity,
        int generatedQuantity,
        int qualifiedQuantity,
        int remainingQuantity,
        List<TireTraceItem> tires
) {
}
