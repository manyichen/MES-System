package com.example.messystem.trace.entity;

import java.nio.file.Path;

public record TraceFileBundle(
        Path qrCode,
        Path labelImage,
        Path pdfDocument
) {
}
