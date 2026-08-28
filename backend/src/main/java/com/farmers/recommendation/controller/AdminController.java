package com.farmers.recommendation.controller;

import com.farmers.recommendation.dto.StatisticsDto;
import com.farmers.recommendation.entity.Scheme;
import com.farmers.recommendation.service.SchemeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
@Tag(name = "Admin APIs", description = "Endpoints for administrator CRUD operations, database reloading, and system statistics")
public class AdminController {

    @Autowired
    private SchemeService schemeService;

    @PostMapping("/scheme")
    @Operation(summary = "Create a new scheme", description = "Creates a new scheme and automatically computes its 384-d embedding locally")
    public ResponseEntity<Scheme> createScheme(@RequestBody Scheme scheme) {
        Scheme saved = schemeService.saveScheme(scheme);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/scheme/{id}")
    @Operation(summary = "Update a scheme", description = "Updates an existing scheme and recalculates its embedding")
    public ResponseEntity<Scheme> updateScheme(@PathVariable UUID id, @RequestBody Scheme scheme) {
        scheme.setId(id);
        Scheme updated = schemeService.saveScheme(scheme);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/scheme/{id}")
    @Operation(summary = "Delete a scheme", description = "Removes a scheme from the database by its UUID ID")
    public ResponseEntity<Map<String, String>> deleteScheme(@PathVariable UUID id) {
        schemeService.deleteScheme(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Scheme deleted successfully with ID: " + id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reload")
    @Operation(summary = "Reload dataset", description = "Clears and reloads the schemes database using the pre-computed schemes_with_embeddings.json file")
    public ResponseEntity<Map<String, Object>> reloadDataset() {
        int count = schemeService.reloadDataset();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Dataset reloaded successfully");
        response.put("totalLoaded", count);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get system statistics", description = "Compiles counts and charts metrics for the dashboard")
    public ResponseEntity<StatisticsDto> getStatistics() {
        return ResponseEntity.ok(schemeService.getStatistics());
    }
}
