package com.farmers.recommendation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.farmers.recommendation.dto.StatisticsDto;
import com.farmers.recommendation.entity.FaqItem;
import com.farmers.recommendation.entity.Scheme;
import com.farmers.recommendation.exception.ResourceNotFoundException;
import com.farmers.recommendation.repository.SchemeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SchemeService {

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${app.data-path}")
    private String dataPath;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Startup listener that automatically loads the dataset into the database
     * if the schemes table is currently empty.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initDatabaseOnStartup() {
        long count = schemeRepository.count();
        if (count == 0) {
            log.info("Database schemes table is empty. Initializing automatic seed loading from: {}", dataPath);
            try {
                reloadDataset();
            } catch (Exception e) {
                log.error("Failed to auto-seed schemes table on startup: {}", e.getMessage(), e);
            }
        } else {
            log.info("Database currently contains {} schemes. Skipping automatic startup seeding.", count);
        }
    }

    public List<Scheme> getAllSchemes() {
        return schemeRepository.findAll();
    }

    public Scheme getSchemeById(UUID id) {
        return schemeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Scheme not found with ID: " + id));
    }

    public List<Scheme> searchSchemes(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllSchemes();
        }
        return schemeRepository.searchByKeyword(keyword.trim());
    }

    /**
     * Creates or updates a scheme, computing its embedding in Java.
     */
    @Transactional
    public Scheme saveScheme(Scheme scheme) {
        if (scheme.getId() == null) {
            scheme.setId(UUID.randomUUID());
        }
        if (scheme.getSlug() == null || scheme.getSlug().trim().isEmpty()) {
            scheme.setSlug(scheme.getName().toLowerCase().replaceAll("[^a-z0-9]+", "-"));
        }

        // Generate embedding based on scheme contents
        String embeddingText = String.format("%s %s %s %s %s %s",
                scheme.getName(),
                scheme.getDescription() != null ? scheme.getDescription() : "",
                scheme.getBenefits() != null ? scheme.getBenefits() : "",
                scheme.getEligibility() != null ? scheme.getEligibility() : "",
                String.join(" ", scheme.getCategory()),
                scheme.getState()
        );
        float[] embedding = embeddingService.getEmbedding(embeddingText);

        // Perform Postgres custom SQL insert/upsert to bind vector and JSONB types
        upsertSchemeInDatabase(scheme, embedding);

        return getSchemeById(scheme.getId());
    }

    @Transactional
    public void deleteScheme(UUID id) {
        Scheme scheme = getSchemeById(id);
        schemeRepository.delete(scheme);
    }

    /**
     * Parses the pre-computed schemes_with_embeddings.json file and upserts it in bulk.
     */
    @Transactional
    public int reloadDataset() {
        log.info("Reloading dataset from: {}", dataPath);
        File file = new File(dataPath);
        if (!file.exists()) {
            // Check in parent path or absolute workspace path as fallback
            file = new File("d:/miniProject2/dataset/schemes_with_embeddings.json");
            if (!file.exists()) {
                throw new RuntimeException("Embedded dataset file not found at configured paths.");
            }
        }

        try {
            // Read JSON file as a list of raw maps to easily access embedding arrays
            List<Map<String, Object>> rawSchemes = objectMapper.readValue(file, new TypeReference<List<Map<String, Object>>>() {});
            log.info("Read {} schemes from JSON file. Inserting into PostgreSQL database...", rawSchemes.size());

            int count = 0;
            for (Map<String, Object> raw : rawSchemes) {
                // Map properties to Scheme entity
                UUID id = UUID.fromString((String) raw.get("id"));
                String slug = (String) raw.get("slug");
                String name = (String) raw.get("name");
                String shortTitle = (String) raw.get("shortTitle");
                String state = (String) raw.get("state");
                String department = (String) raw.get("department");
                String level = (String) raw.get("level");
                List<String> category = castList(raw.get("category"));
                List<String> subcategory = castList(raw.get("subcategory"));
                String beneficiary = (String) raw.get("beneficiary");
                String description = (String) raw.get("description");
                String benefits = (String) raw.get("benefits");
                String eligibility = (String) raw.get("eligibility");
                List<String> documents = castList(raw.get("documents"));
                String appMode = (String) raw.get("applicationMode");
                String appLink = (String) raw.get("applicationLink");
                
                // Map FAQs
                List<Map<String, String>> rawFaqs = (List<Map<String, String>>) raw.get("faqs");
                List<FaqItem> faqs = new ArrayList<>();
                if (rawFaqs != null) {
                    for (Map<String, String> f : rawFaqs) {
                        faqs.add(new FaqItem(f.get("question"), f.get("answer")));
                    }
                }

                // Map embedding
                List<Double> doubleList = (List<Double>) raw.get("embedding");
                float[] embedding = new float[doubleList.size()];
                for (int i = 0; i < doubleList.size(); i++) {
                    embedding[i] = doubleList.get(i).floatValue();
                }

                Scheme scheme = Scheme.builder()
                        .id(id)
                        .slug(slug)
                        .name(name)
                        .shortTitle(shortTitle)
                        .state(state)
                        .department(department)
                        .level(level)
                        .category(category)
                        .subcategory(subcategory)
                        .beneficiary(beneficiary)
                        .description(description)
                        .benefits(benefits)
                        .eligibility(eligibility)
                        .documents(documents)
                        .applicationMode(appMode)
                        .applicationLink(appLink)
                        .faq(faqs)
                        .build();

                upsertSchemeInDatabase(scheme, embedding);
                count++;
            }

            log.info("Bulk loaded {} schemes with embeddings successfully.", count);
            return count;

        } catch (IOException e) {
            log.error("Failed to read embedding dataset file: {}", e.getMessage(), e);
            throw new RuntimeException("Error reading scheme embedding file: " + e.getMessage(), e);
        }
    }

    /**
     * Compiles statistical counts and aggregations for the schemes.
     */
    public StatisticsDto getStatistics() {
        List<Scheme> all = schemeRepository.findAll();
        
        long total = all.size();
        long central = all.stream().filter(s -> "Central".equalsIgnoreCase(s.getLevel())).count();
        long state = total - central;

        Map<String, Long> byState = all.stream()
                .collect(Collectors.groupingBy(Scheme::getState, Collectors.counting()));

        Map<String, Long> byCategory = new HashMap<>();
        for (Scheme s : all) {
            for (String cat : s.getCategory()) {
                byCategory.put(cat, byCategory.getOrDefault(cat, 0L) + 1);
            }
        }

        return StatisticsDto.builder()
                .totalSchemes(total)
                .centralSchemes(central)
                .stateSchemes(state)
                .schemesByState(byState)
                .schemesByCategory(byCategory)
                .build();
    }

    /**
     * Executes native SQL upsert statement using JdbcTemplate.
     * Maps vectors as Strings formatted as arrays, and lists/maps as serialized JSON strings.
     */
    private void upsertSchemeInDatabase(Scheme scheme, float[] embedding) {
        String sql = "INSERT INTO schemes (" +
                "  id, slug, name, short_title, state, department, level, " +
                "  category, subcategory, beneficiary, description, benefits, eligibility, " +
                "  documents, application_mode, application_link, faq, embedding" +
                ") VALUES (" +
                "  ?, ?, ?, ?, ?, ?, ?, " +
                "  ?::jsonb, ?::jsonb, ?, ?, ?, ?, " +
                "  ?::jsonb, ?, ?, ?::jsonb, CAST(? AS vector)" +
                ") ON CONFLICT (id) DO UPDATE SET " +
                "  slug = EXCLUDED.slug, " +
                "  name = EXCLUDED.name, " +
                "  short_title = EXCLUDED.short_title, " +
                "  state = EXCLUDED.state, " +
                "  department = EXCLUDED.department, " +
                "  level = EXCLUDED.level, " +
                "  category = EXCLUDED.category, " +
                "  subcategory = EXCLUDED.subcategory, " +
                "  beneficiary = EXCLUDED.beneficiary, " +
                "  description = EXCLUDED.description, " +
                "  benefits = EXCLUDED.benefits, " +
                "  eligibility = EXCLUDED.eligibility, " +
                "  documents = EXCLUDED.documents, " +
                "  application_mode = EXCLUDED.application_mode, " +
                "  application_link = EXCLUDED.application_link, " +
                "  faq = EXCLUDED.faq, " +
                "  embedding = EXCLUDED.embedding";

        try {
            String categoryJson = objectMapper.writeValueAsString(scheme.getCategory());
            String subcategoryJson = objectMapper.writeValueAsString(scheme.getSubcategory());
            String documentsJson = objectMapper.writeValueAsString(scheme.getDocuments());
            String faqJson = objectMapper.writeValueAsString(scheme.getFaq());
            String vectorStr = embeddingService.getVectorString(embedding);

            jdbcTemplate.update(sql,
                    scheme.getId(),
                    scheme.getSlug(),
                    scheme.getName(),
                    scheme.getShortTitle(),
                    scheme.getState(),
                    scheme.getDepartment(),
                    scheme.getLevel(),
                    categoryJson,
                    subcategoryJson,
                    scheme.getBeneficiary(),
                    scheme.getDescription(),
                    scheme.getBenefits(),
                    scheme.getEligibility(),
                    documentsJson,
                    scheme.getApplicationMode(),
                    scheme.getApplicationLink(),
                    faqJson,
                    vectorStr
            );
        } catch (Exception e) {
            log.error("Failed to upsert scheme '{}' with custom types: {}", scheme.getName(), e.getMessage(), e);
            throw new RuntimeException("Database upsert failed for scheme: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> castList(Object obj) {
        if (obj instanceof List) {
            return (List<String>) obj;
        }
        return new ArrayList<>();
    }
}
