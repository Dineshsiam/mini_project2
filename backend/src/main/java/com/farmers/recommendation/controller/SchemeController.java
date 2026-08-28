package com.farmers.recommendation.controller;

import com.farmers.recommendation.entity.Scheme;
import com.farmers.recommendation.service.SchemeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/schemes")
@CrossOrigin(origins = "*")
@Tag(name = "User Scheme APIs", description = "Endpoints for fetching and searching government schemes")
public class SchemeController {

    @Autowired
    private SchemeService schemeService;

    @GetMapping
    @Operation(summary = "Get all schemes", description = "Retrieves a complete list of preprocessed schemes")
    public ResponseEntity<List<Scheme>> getAllSchemes() {
        return ResponseEntity.ok(schemeService.getAllSchemes());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get scheme details", description = "Retrieves specific scheme details by its UUID ID")
    public ResponseEntity<Scheme> getSchemeById(@PathVariable UUID id) {
        return ResponseEntity.ok(schemeService.getSchemeById(id));
    }

    @GetMapping("/search")
    @Operation(summary = "Search schemes", description = "Performs text search across scheme name, description, benefits, and eligibility criteria")
    public ResponseEntity<List<Scheme>> searchSchemes(@RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(schemeService.searchSchemes(keyword));
    }
}
