package edu.software.project.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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
    void fullComponentLifecycleFlowWorks() throws Exception {
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

        String componentResponse = mockMvc.perform(post("/api/components")
                        .header("X-Auth-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Order UML",
                                  "description": "Class diagram for ordering",
                                  "keywords": "uml,order,design",
                                  "type": "UML",
                                  "catalogueIds": [%d]
                                }
                                """.formatted(catalogueId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Order UML"))
                .andExpect(jsonPath("$.catalogueIds[0]").value(catalogueId))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long componentId = objectMapper.readTree(componentResponse).get("id").asLong();

        mockMvc.perform(get("/api/components/search")
                        .header("X-Auth-Token", token)
                        .param("keywords", "uml"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(componentId))
                .andExpect(jsonPath("$[0].searchHitCount").value(1));

        mockMvc.perform(post("/api/components/{id}/use", componentId)
                        .header("X-Auth-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usageCount").value(1))
                .andExpect(jsonPath("$.searchedButNotUsedCount").value(0));

        mockMvc.perform(get("/api/users/me")
                        .header("X-Auth-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@example.com"));
    }

    @Test
    void protectedEndpointsRequireToken() throws Exception {
        mockMvc.perform(get("/api/components"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Missing X-Auth-Token header"));
    }
}
