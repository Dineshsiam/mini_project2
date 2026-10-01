package com.farmers.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for the scheme sync admin endpoint.
 * The backend calculates a SHA-256 hash from the content fields
 * and only updates + re-embeds if the hash has changed.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemeSyncRequest {

    private String schemeId;        // Used as slug / external key
    private String title;
    private String description;
    private String eligibility;
    private String benefits;
    private String sourceUrl;
    private String state;
    private String level;           // Central | State
    private String department;
    private String contentHash;     // Provided by caller; if null, computed by backend
}
