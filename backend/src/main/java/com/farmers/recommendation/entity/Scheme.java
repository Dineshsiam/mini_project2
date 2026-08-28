package com.farmers.recommendation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "schemes")
public class Scheme {

    @Id
    private UUID id;

    @Column(unique = true, nullable = false)
    private String slug;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String name;

    @Column(name = "short_title", columnDefinition = "TEXT")
    private String shortTitle;

    @Column(nullable = false)
    private String state;

    @Column(columnDefinition = "TEXT")
    private String department;

    private String level;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<String> category = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "subcategory", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> subcategory = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String beneficiary;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String benefits;

    @Column(columnDefinition = "TEXT")
    private String eligibility;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<String> documents = new ArrayList<>();

    @Column(name = "application_mode", columnDefinition = "TEXT")
    private String applicationMode;

    @Column(name = "application_link", columnDefinition = "TEXT")
    private String applicationLink;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "faq", columnDefinition = "jsonb")
    @Builder.Default
    private List<FaqItem> faq = new ArrayList<>();

    // The embedding vector is kept transient in standard JPA mappings
    // because standard JPA drivers do not support native PostgreSQL vector bindings.
    // We handle vector loading/searching through JDBC custom native SQL queries.
    @Transient
    private float[] embedding;
}
