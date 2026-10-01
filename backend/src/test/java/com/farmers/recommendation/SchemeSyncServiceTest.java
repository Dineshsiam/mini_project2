package com.farmers.recommendation;

import com.farmers.recommendation.dto.SchemeSyncRequest;
import com.farmers.recommendation.entity.Scheme;
import com.farmers.recommendation.repository.SchemeRepository;
import com.farmers.recommendation.service.SchemeService;
import com.farmers.recommendation.service.SchemeSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SchemeSyncServiceTest {

    @Mock
    private SchemeRepository schemeRepository;

    @Mock
    private SchemeService schemeService;

    @InjectMocks
    private SchemeSyncService schemeSyncService;

    private Scheme existingScheme;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        existingScheme = new Scheme();
        existingScheme.setId(UUID.randomUUID());
        existingScheme.setSlug("pm-kisan");
        existingScheme.setName("PM Kisan");
        existingScheme.setContentHash("dummyHash");
    }

    @Test
    void testSyncScheme_noUpdateWhenHashMatches() {
        when(schemeRepository.findAll()).thenReturn(List.of(existingScheme));

        SchemeSyncRequest req = new SchemeSyncRequest();
        req.setSchemeId("pm-kisan");
        req.setContentHash("dummyHash"); // Same hash

        String result = schemeSyncService.syncScheme(req);

        assertThat(result).isEqualTo("SKIPPED - Hash matched");
        verify(schemeService, never()).saveScheme(any(Scheme.class));
    }

    @Test
    void testSyncScheme_updatesWhenHashDiffers() {
        when(schemeRepository.findAll()).thenReturn(List.of(existingScheme));

        SchemeSyncRequest req = new SchemeSyncRequest();
        req.setSchemeId("pm-kisan");
        req.setContentHash("newHash"); // Different hash

        String result = schemeSyncService.syncScheme(req);

        assertThat(result).isEqualTo("UPDATED - Hash mismatched");
        verify(schemeService, times(1)).saveScheme(any(Scheme.class));
    }

    @Test
    void testSyncScheme_createsWhenNewSlug() {
        // Empty DB
        when(schemeRepository.findAll()).thenReturn(List.of());

        SchemeSyncRequest req = new SchemeSyncRequest();
        req.setSchemeId("new-scheme");
        req.setContentHash("hash123");

        String result = schemeSyncService.syncScheme(req);

        assertThat(result).isEqualTo("CREATED - New scheme");
        verify(schemeService, times(1)).saveScheme(any(Scheme.class));
    }
}
