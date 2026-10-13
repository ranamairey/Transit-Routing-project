package com.example.demo.transit.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.demo.transit.repository.UserRepository;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test of the REST API on the seeded demo network, against an embedded MongoDB.
 * Covers FR-1.x (routing), FR-2.x (reports/alerts), FR-3.x (admin) and NFR-3.1 (no login to search).
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TransitApiIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    UserRepository userRepository;

    String adminToken;
    String userToken;
    String qatanaId;
    String bridgeId;
    String qatanaRouteId;

    @BeforeAll
    void login() throws Exception {
        adminToken = token("admin@demo.com", "Admin#12345");
        userToken = token("ahmad@demo.com", "User#12345");
        JsonNode stations = json.readTree(mvc.perform(get("/api/v1/stations")).andReturn().getResponse().getContentAsString());
        for (JsonNode s : stations) {
            if (s.get("name").asText().equals("قطنا المركز")) qatanaId = s.get("id").asText();
            if (s.get("name").asText().equals("جسر الرئيس")) bridgeId = s.get("id").asText();
        }
        JsonNode routes = json.readTree(mvc.perform(get("/api/v1/routes")).andReturn().getResponse().getContentAsString());
        for (JsonNode r : routes) {
            if (r.get("code").asText().equals("R-QAT-01")) qatanaRouteId = r.get("id").asText();
        }
    }

    private String token(String email, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    @Order(1)
    void searchByCoordinatesNeedsNoLoginAndReturnsMultiLegTrip() throws Exception {
        // Qatana centre -> President bridge (the reference example): servis to Somaria, bus to the bridge.
        String body = """
                {"origin":{"lat":33.4361,"lng":36.0825},"destination":{"lat":33.511,"lng":36.289},"peak":false}
                """;
        MvcResult result = mvc.perform(post("/api/v1/routing/search").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths", hasSize(greaterThan(0))))
                .andReturn();
        JsonNode response = json.readTree(result.getResponse().getContentAsString());
        JsonNode best = response.get("paths").get(0);
        assertEquals(3500.0, best.get("totalFare").asDouble(), 0.001);
        assertEquals(1, best.get("transfers").asInt());

        List<String> rideRoutes = new ArrayList<>();
        int legSum = 0;
        for (JsonNode leg : best.get("legs")) {
            legSum += leg.get("durationSeconds").asInt();
            if (leg.get("type").asText().equals("RIDE")) rideRoutes.add(leg.get("routeName").asText());
        }
        assertEquals(List.of("قطنا - السومرية", "السومرية - البرامكة - جسر الرئيس"), rideRoutes);
        assertEquals(legSum, best.get("totalDurationSeconds").asInt(), "total equals the sum of its legs");
        assertTrue(best.get("totalDurationSeconds").asInt() < 3 * 3600, "no inflated durations");

        // Alternatives are ordered by duration and all have real durations.
        int previous = 0;
        for (JsonNode path : response.get("paths")) {
            int d = path.get("totalDurationSeconds").asInt();
            assertTrue(d >= previous);
            assertTrue(d < 6 * 3600, "alternative duration must not include the ranking penalty: " + d);
            previous = d;
        }
        assertTrue(response.get("processingMillis").asLong() < 1500, "NFR-1.3");
    }

    @Test
    @Order(2)
    void searchByTextAndStationId() throws Exception {
        String body = "{\"originQuery\":\"قطنا\",\"destinationStationId\":\"" + bridgeId + "\",\"peak\":true}";
        mvc.perform(post("/api/v1/routing/search").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originName").value("قطنا المركز"))
                .andExpect(jsonPath("$.destinationName").value("جسر الرئيس"))
                .andExpect(jsonPath("$.peak").value(true))
                .andExpect(jsonPath("$.paths[0].legs[0].type").value("RIDE"));

        mvc.perform(get("/api/v1/stations/search").param("q", "الجسر"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("جسر الرئيس"));
    }

    @Test
    @Order(3)
    void networkSnapshotSupportsEtag() throws Exception {
        MvcResult first = mvc.perform(get("/api/v1/network"))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.routes", hasSize(greaterThan(0))))
                .andReturn();
        String etag = first.getResponse().getHeader("ETag");
        mvc.perform(get("/api/v1/network").header("If-None-Match", etag))
                .andExpect(status().isNotModified());
    }

    @Test
    @Order(4)
    void reportsRequireLoginAndAutoApplyAfterThreeDistinctUsers() throws Exception {
        String report = "{\"reportType\":\"CROWDING\",\"routeId\":\"" + qatanaRouteId + "\",\"description\":\"ازدحام\"}";

        mvc.perform(post("/api/v1/reports").contentType(MediaType.APPLICATION_JSON).content(report))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/v1/reports").header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON).content(report))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.autoApplied").value(false));

        String samerToken = token("samer@demo.com", "User#12345");
        mvc.perform(post("/api/v1/reports").header("Authorization", "Bearer " + samerToken)
                        .contentType(MediaType.APPLICATION_JSON).content(report))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.autoApplied").value(false));

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Third User\",\"email\":\"third@demo.com\",\"password\":\"Third#12345\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
        String thirdToken = token("third@demo.com", "Third#12345");
        mvc.perform(post("/api/v1/reports").header("Authorization", "Bearer " + thirdToken)
                        .contentType(MediaType.APPLICATION_JSON).content(report))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.autoApplied").value(true));

        mvc.perform(get("/api/v1/routes/" + qatanaRouteId))
                .andExpect(jsonPath("$.status").value("CROWDED"));

        // The search now surfaces the alerts and marks the leg as crowded (FR-2.2).
        String body = "{\"originStationId\":\"" + qatanaId + "\",\"destinationStationId\":\"" + bridgeId + "\",\"peak\":false}";
        MvcResult result = mvc.perform(post("/api/v1/routing/search").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alerts", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.paths[0].legs[0].routeStatus").value("CROWDED"))
                .andReturn();
        int crowded = json.readTree(result.getResponse().getContentAsString()).get("paths").get(0).get("legs").get(0).get("durationSeconds").asInt();
        assertTrue(crowded > 2110, "crowded route is slowed down, got " + crowded);
    }

    @Test
    @Order(5)
    void adminEndpointsAreRoleProtected() throws Exception {
        mvc.perform(get("/api/v1/admin/reports/summary")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/reports/summary").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        MvcResult summary = mvc.perform(get("/api/v1/admin/reports/summary").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode groups = json.readTree(summary.getResponse().getContentAsString());
        assertTrue(groups.size() > 0);

        MvcResult list = mvc.perform(get("/api/v1/admin/reports").param("type", "FARE_CHANGE")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode fareReports = json.readTree(list.getResponse().getContentAsString()).get("items");
        assertEquals(1, fareReports.size());
        String fareReportId = fareReports.get(0).get("id").asText();

        // Approve: fare becomes 2500 on the Qatana line.
        mvc.perform(post("/api/v1/admin/reports/" + fareReportId + "/approve").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applied").value(true));
        mvc.perform(get("/api/v1/routes/" + qatanaRouteId))
                .andExpect(jsonPath("$.currentFare").value(2500.0))
                .andExpect(jsonPath("$.status").value("FARE_UPDATED"));

        // Reset the route status manually.
        mvc.perform(patch("/api/v1/admin/routes/" + qatanaRouteId + "/status").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"NORMAL\",\"currentFare\":2000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NORMAL"));
    }

    @Test
    @Order(8)
    void englishMessagesPasswordResetAndEmailVerification() throws Exception {
        // Accept-Language: en translates API errors.
        mvc.perform(get("/api/v1/routes/does-not-exist").header("Accept-Language", "en-US"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Line not found"));
        mvc.perform(get("/api/v1/routes/does-not-exist"))
                .andExpect(jsonPath("$.error").value("الخط غير موجود"));

        // New rider: not verified until the emailed token is used.
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Reset Tester\",\"email\":\"reset@demo.com\",\"password\":\"Reset#123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.emailVerified").value(false));
        String verifyToken = userRepository.findByEmail("reset@demo.com").orElseThrow().getVerificationToken();
        assertTrue(verifyToken != null && verifyToken.length() > 20);
        mvc.perform(post("/api/v1/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + verifyToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailVerified").value(true));
        mvc.perform(post("/api/v1/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + verifyToken + "\"}"))
                .andExpect(status().isBadRequest());

        // Forgot password never reveals whether the email exists; the token lands in the user document.
        mvc.perform(post("/api/v1/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@demo.com\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"reset@demo.com\"}"))
                .andExpect(status().isOk());
        String resetToken = userRepository.findByEmail("reset@demo.com").orElseThrow().getResetToken();
        mvc.perform(post("/api/v1/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + resetToken + "\",\"newPassword\":\"Newpass1\"}"))
                .andExpect(status().isOk());
        token("reset@demo.com", "Newpass1");
        mvc.perform(post("/api/v1/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + resetToken + "\",\"newPassword\":\"Newpass2\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("رابط غير صالح أو منتهي الصلاحية"));
    }

    @Test
    @Order(6)
    void adminCanManageStationsAndRoutes() throws Exception {
        MvcResult created = mvc.perform(post("/api/v1/admin/stations").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"محطة اختبار\",\"aliases\":\"اختبار\",\"location\":{\"lat\":33.515,\"lng\":36.29}}"))
                .andExpect(status().isCreated())
                .andReturn();
        String stationId = json.readTree(created.getResponse().getContentAsString()).get("id").asText();

        MvcResult route = mvc.perform(post("/api/v1/admin/routes").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"خط اختبار\",\"code\":\"R-TEST\",\"vehicleType\":\"SERVIS\",\"currentFare\":500,"
                                + "\"stationIds\":[\"" + bridgeId + "\",\"" + stationId + "\"],"
                                + "\"polyline\":[{\"lat\":33.511,\"lng\":36.289},{\"lat\":33.513,\"lng\":36.2895},{\"lat\":33.515,\"lng\":36.29}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.polyline", hasSize(3)))
                .andExpect(jsonPath("$.stops", hasSize(2)))
                .andReturn();
        String routeId = json.readTree(route.getResponse().getContentAsString()).get("id").asText();

        // Station in use cannot be deleted.
        mvc.perform(delete("/api/v1/admin/stations/" + stationId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());

        // Search can now reach the new station via the new line.
        String body = "{\"originStationId\":\"" + qatanaId + "\",\"destinationStationId\":\"" + stationId + "\",\"peak\":false}";
        mvc.perform(post("/api/v1/routing/search").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths[0].legs[-1].toName").value("محطة اختبار"));

        mvc.perform(delete("/api/v1/admin/routes/" + routeId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/admin/stations/" + stationId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
        assertFalse(mvc.perform(get("/api/v1/stations")).andReturn().getResponse().getContentAsString().contains("محطة اختبار"));
    }
}
