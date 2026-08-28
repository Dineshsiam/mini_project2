package com.farmers.recommendation.repository;

import com.farmers.recommendation.entity.Scheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SchemeRepository extends JpaRepository<Scheme, UUID> {

    @Query("SELECT s FROM Scheme s WHERE " +
           "LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.benefits) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.eligibility) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Scheme> searchByKeyword(@Param("keyword") String keyword);

    /**
     * Finds the nearest neighbor IDs and their cosine similarity score.
     * Vector distance <=> computes cosine distance. Cosine similarity = 1 - cosine distance.
     * We pass the query embedding as a string representation like '[0.1, 0.2, ...]'
     */
    @Query(value = "SELECT id, (1 - (embedding <=> CAST(:vectorStr AS vector))) AS similarity " +
                   "FROM schemes " +
                   "ORDER BY embedding <=> CAST(:vectorStr AS vector) " +
                   "LIMIT :limit", nativeQuery = true)
    List<Object[]> findNearestNeighborIdsWithSimilarity(@Param("vectorStr") String vectorStr, @Param("limit") int limit);
}
