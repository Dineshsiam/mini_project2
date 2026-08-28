package com.farmers.recommendation;

import com.farmers.recommendation.repository.SchemeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration"
})
class RecommendationApplicationTests {

    @MockBean
    private SchemeRepository schemeRepository;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoads() {
        System.out.println("Spring Boot application context loaded successfully with mocked DB beans.");
    }
}
