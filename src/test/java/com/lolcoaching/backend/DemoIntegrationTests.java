package com.lolcoaching.backend;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.lolcoaching.backend.service.MatchService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:portfolio_demo;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "cors.allowed-origin=http://localhost",
    "app.demo.read-only=true"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DemoIntegrationTests {
    @Autowired MatchService matches;
    @Autowired TestRestTemplate http;
    long matchId;

    @BeforeAll
    void seedSyntheticMatchThroughTheRealImporter() throws Exception {
        var zip = new MockMultipartFile("zipFile", "portfolio-demo.zip", "application/zip",
            Files.readAllBytes(Path.of("examples/portfolio-demo.zip")));
        matchId = matches.importMatch(zip, "SYNTHETIC-DEMO", "BLUE");
    }

    @Test
    void returnsSyntheticMatchAndEveryPattern() {
        var detail = http.getForEntity("/api/matches/" + matchId, JsonNode.class);
        assertEquals(200, detail.getStatusCode().value());
        var body = detail.getBody();
        assertNotNull(body);
        assertEquals("SYNTHETIC-DEMO", body.path("matchCode").asText());
        assertEquals(10, body.path("players").size());
        assertEquals(24, body.path("voiceLogs").size());
        assertEquals(4, body.path("gameEvents").size());
        assertEquals(120000, body.path("duration").asLong());
        for (int[] pair : new int[][]{{1,0}, {2,3}, {0,0}, {0,1}, {0,2}, {3,0}, {-1,-1}}) {
            var metrics = http.getForEntity("/api/matches/" + matchId + "/metrics?sourceDa="
                + pair[0] + "&targetDa=" + pair[1], JsonNode.class);
            assertEquals(200, metrics.getStatusCode().value());
            assertNotNull(metrics.getBody());
            assertFalse(metrics.getBody().isEmpty());
        }
        var range = http.getForEntity("/api/matches/" + matchId
            + "/analysis?start=0&end=60&sourceDa=1&targetDa=0", JsonNode.class);
        assertEquals(200, range.getStatusCode().value());
        assertTrue(range.getBody().path("density").isNumber());
    }

    @Test
    void preventsPublicUploadAndDeletionWithoutChangingData() {
        var create = http.postForEntity("/api/matches/import", HttpEntity.EMPTY, String.class);
        assertEquals(403, create.getStatusCode().value());
        var delete = http.exchange("/api/matches/" + matchId, HttpMethod.DELETE,
            HttpEntity.EMPTY, String.class);
        assertEquals(403, delete.getStatusCode().value());
        var list = http.getForEntity("/api/matches/list", JsonNode.class);
        assertEquals(200, list.getStatusCode().value());
        assertEquals(1, list.getBody().size());
    }
}
