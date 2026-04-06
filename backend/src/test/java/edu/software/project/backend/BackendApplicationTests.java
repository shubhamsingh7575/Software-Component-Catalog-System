package edu.software.project.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BackendApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullCatalogueLifecycleFlowWorks() throws Exception {
        String registerResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "Admin User",
                                  "email": "admin@example.com",
                                  "password": "StrongPass1!",
                                  "confirmPassword": "StrongPass1!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode authJson = objectMapper.readTree(registerResponse);
        String token = authJson.get("token").asText();

        String catalogueResponse = mockMvc.perform(post("/api/catalogues")
                        .header("X-Auth-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Design Assets",
                                  "description": "Reusable design artifacts",
                                  "keywords": "uml, erd"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Design Assets"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long catalogueId = objectMapper.readTree(catalogueResponse).get("id").asLong();

        String componentResponse = mockMvc.perform(post("/api/catalogues/{catalogueId}/components", catalogueId)
                        .header("X-Auth-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Order UML",
                                  "description": "Class diagram for ordering",
                                  "keywords": "uml,order,design",
                                  "type": "UML"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Order UML"))
                .andExpect(jsonPath("$.catalogueId").value(catalogueId))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long componentId = objectMapper.readTree(componentResponse).get("id").asLong();

        mockMvc.perform(get("/api/catalogues")
                        .header("X-Auth-Token", token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(catalogueId))
                .andExpect(jsonPath("$[0].components[0].id").value(componentId));

        mockMvc.perform(post("/api/catalogues/{catalogueId}/components/{componentId}/use", catalogueId, componentId)
                        .header("X-Auth-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usageCount").value(1));

        mockMvc.perform(get("/api/users/me")
                        .header("X-Auth-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@example.com"));
    }

    @Test
    void logoutInvalidatesActiveSession() throws Exception {
        String registerResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "Admin User",
                                  "email": "admin@example.com",
                                  "password": "StrongPass1!",
                                  "confirmPassword": "StrongPass1!"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(registerResponse).get("token").asText();

        mockMvc.perform(post("/api/auth/logout")
                        .header("X-Auth-Token", token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/me")
                        .header("X-Auth-Token", token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired session"));
    }

    @Test
    void protectedEndpointsRequireToken() throws Exception {
        mockMvc.perform(get("/api/catalogues"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Missing X-Auth-Token header"));
    }

    @Test
    void duplicateRegistrationReturnsConflictWithMeaningfulMessage() throws Exception {
        String payload = """
                {
                  "username": "Admin User",
                  "email": "admin@example.com",
                  "password": "StrongPass1!",
                  "confirmPassword": "StrongPass1!"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "An account with this email already exists. Sign in instead or use a different email address."));
    }
}
