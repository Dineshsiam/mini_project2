package com.farmers.recommendation.controller;

import com.farmers.recommendation.dto.SchemeSyncRequest;
import com.farmers.recommendation.service.SchemeSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/schemes")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Tag(name = "Admin Scheme Sync API", description = "Endpoint for syncing government schemes with hash-based update detection")
public class AdminSchemeController {

    private final SchemeSyncService schemeSyncService;

    @PostMapping("/sync")
    @Operation(summary = "Sync a scheme", description = "Accepts a scheme payload, checks its hash against the DB, and updates/re-embeds it if changed.")
    public ResponseEntity<Map<String, String>> syncScheme(@RequestBody SchemeSyncRequest request) {
        String result = schemeSyncService.syncScheme(request);
        return ResponseEntity.ok(Map.of(
                "schemeId", request.getSchemeId(),
                "status", result
        ));
    }
}
