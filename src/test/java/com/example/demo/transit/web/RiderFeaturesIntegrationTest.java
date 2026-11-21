package com.example.demo.transit.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Saved places, followed lines, live line status, rider suggestions and usage statistics. */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RiderFeaturesIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;

    String adminToken;
    String userToken;
    String bridgeId;
    String baramkehId;
    String muhajireenRouteId;

    @BeforeAll
    void setUp() throws Exception {
        adminToken = token("admin@demo.com", "Admin#12345");
        userToken = token("samer@demo.com", "User#12345");
        for (JsonNode s : read(get("/api/v1/stations"))) {
            if (s.get("name").asText().equals("جسر الرئيس")) bridgeId = s.get("id").asText();
            if (s.get("name").asText().equals("البرامكة")) baramkehId = s.get("id").asText();
        }
        for (JsonNode r : read(get("/api/v1/routes"))) {
            if (r.get("code").asText().equals("R-MUH-01")) muhajireenRouteId = r.get("id").asText();
        }
    }

    @Test
    void savedPlacesFollowTheAccount() throws Exception {
        mvc.perform(get("/api/v1/me/places")).andExpect(status().isUnauthorized());
        String places = "[{\"kind\":\"HOME\",\"label\":\"البيت\",\"name\":\"جسر الرئيس\",\"stationId\":\"" + bridgeId + "\"},"
                + "{\"kind\":\"WORK\",\"label\":\"العمل\",\"name\":\"دبوس\",\"lat\":33.5,\"lng\":36.28}]";
        mvc.perform(put("/api/v1/me/places").header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON).content(places))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").isNotEmpty());
        mvc.perform(get("/api/v1/me/places").header("Authorization", "Bearer " + userToken))
                .andExpect(jsonPath("$[1].kind").value("WORK"))
                .andExpect(jsonPath("$[1].lat").value(33.5));
        // A place needs a stop or a point.
        mvc.perform(put("/api/v1/me/places").header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON).content("[{\"kind\":\"HOME\",\"name\":\"x\"}]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void followedLinesGetLiveAlerts() throws Exception {
        mvc.perform(put("/api/v1/me/follows").header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON).content("[\"" + muhajireenRouteId + "\",\"unknown-line\"]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mvc.perform(post("/api/v1/reports").header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"routeId\":\"" + muhajireenRouteId + "\",\"reportType\":\"DELAY\",\"description\":\"تأخير\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/me/alerts").header("Authorization", "Bearer " + userToken))
                .andExpect(jsonPath("$[*].routeId", hasItem(muhajireenRouteId)));
        mvc.perform(get("/api/v1/reports/live"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.routeId == '" + muhajireenRouteId + "')].reportCounts.DELAY", hasItem(greaterThanOrEqualTo(1))));
        mvc.perform(get("/api/v1/push/key")).andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void acceptedMissingPlaceBecomesAStation() throws Exception {
        mvc.perform(post("/api/v1/suggestions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"MISSING_PLACE\",\"name\":\"الشعلان\",\"location\":{\"lat\":33.5186,\"lng\":36.2905}}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/suggestions").header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"MISSING_PLACE\",\"name\":\"الشعلان\"}"))
                .andExpect(status().isBadRequest());
        JsonNode created = json.readTree(mvc.perform(post("/api/v1/suggestions").header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"MISSING_PLACE\",\"name\":\"الشعلان\",\"nameEn\":\"Shaalan\",\"location\":{\"lat\":33.5186,\"lng\":36.2905}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString());

        mvc.perform(get("/api/v1/admin/suggestions?status=PENDING").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/suggestions?status=PENDING").header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$[*].id", hasItem(created.get("id").asText())));
        mvc.perform(post("/api/v1/admin/suggestions/" + created.get("id").asText() + "/accept").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.resultStationId").isNotEmpty());

        assertTrue(mvc.perform(get("/api/v1/stations")).andReturn().getResponse().getContentAsString().contains("Shaalan"));
    }

    @Test
    void searchesAreCountedForTheAdmin() throws Exception {
        String body = "{\"originStationId\":\"" + baramkehId + "\",\"destinationStationId\":\"" + bridgeId + "\",\"peak\":false}";
        mvc.perform(post("/api/v1/routing/search").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
        long searches = 0;
        for (int i = 0; i < 50 && searches == 0; i++) {
            Thread.sleep(100);
            searches = read(get("/api/v1/admin/stats/usage?days=7").header("Authorization", "Bearer " + adminToken)).get("searches").asLong();
        }
        assertTrue(searches >= 1);
        mvc.perform(get("/api/v1/admin/stats/usage?days=7").header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.perDay", hasSize(7)))
                .andExpect(jsonPath("$.topTrips[*].destination", hasItem("جسر الرئيس")));
    }

    private JsonNode read(org.springframework.test.web.servlet.RequestBuilder request) throws Exception {
        return json.readTree(mvc.perform(request).andReturn().getResponse().getContentAsString());
    }

    private String token(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }
}
