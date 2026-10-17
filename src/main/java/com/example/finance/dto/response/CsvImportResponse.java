package com.example.finance.dto.response;

import java.util.List;

public record CsvImportResponse(
        int totalRows,
        int importedCount,
        int skippedCount,
        List<String> errors,
        String message
) {
}
