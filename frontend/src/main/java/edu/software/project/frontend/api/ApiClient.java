package edu.software.project.frontend.api;

import edu.software.project.frontend.model.AuthResponse;
import edu.software.project.frontend.model.Catalogue;
import edu.software.project.frontend.model.CatalogueRequest;
import edu.software.project.frontend.model.Component;
import edu.software.project.frontend.model.ComponentListItem;
import edu.software.project.frontend.model.ComponentRequest;
import edu.software.project.frontend.model.ComponentType;
import edu.software.project.frontend.model.LoginRequest;
import edu.software.project.frontend.model.RegisterRequest;
import edu.software.project.frontend.model.Role;
import edu.software.project.frontend.model.UserProfile;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ApiClient {
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public AuthResponse register(String baseUrl, RegisterRequest request) {
        return toAuthResponse(send(baseUrl, "POST", "/api/auth/register", null, mapOf(
                "username", request.username(),
                "email", request.email(),
                "password", request.password(),
                "confirmPassword", request.confirmPassword()
        )));
    }

    public AuthResponse login(String baseUrl, LoginRequest request) {
        return toAuthResponse(send(baseUrl, "POST", "/api/auth/login", null, mapOf(
                "email", request.email(),
                "password", request.password()
        )));
    }

    public void logout(String baseUrl, String token) {
        send(baseUrl, "POST", "/api/auth/logout", token, null);
    }

    public UserProfile getCurrentUser(String baseUrl, String token) {
        return toUserProfile(asMap(send(baseUrl, "GET", "/api/users/me", token, null)));
    }

    public List<Catalogue> getCatalogues(String baseUrl, String token) {
        return toCatalogueList(send(baseUrl, "GET", "/api/catalogues", token, null));
    }

    public List<Catalogue> getMyCatalogues(String baseUrl, String token) {
        return toCatalogueList(send(baseUrl, "GET", "/api/catalogues/mine", token, null));
    }

    public Catalogue createCatalogue(String baseUrl, String token, CatalogueRequest request) {
        return toCatalogue(asMap(send(baseUrl, "POST", "/api/catalogues", token, mapOf(
                "name", request.name(),
                "description", request.description(),
                "keywords", request.keywords()
        ))));
    }

    public Catalogue updateCatalogue(String baseUrl, String token, long id, CatalogueRequest request) {
        return toCatalogue(asMap(send(baseUrl, "PUT", "/api/catalogues/" + id, token, mapOf(
                "name", request.name(),
                "description", request.description(),
                "keywords", request.keywords()
        ))));
    }

    public void deleteCatalogue(String baseUrl, String token, long id) {
        send(baseUrl, "DELETE", "/api/catalogues/" + id, token, null);
    }

    public List<Component> getComponents(String baseUrl, String token) {
        return toComponentList(send(baseUrl, "GET", "/api/components", token, null));
    }

    public Component getComponent(String baseUrl, String token, long id) {
        return toComponent(asMap(send(baseUrl, "GET", "/api/components/" + id, token, null)));
    }

    public List<Component> searchComponents(String baseUrl, String token, String keywords) {
        String encoded = URLEncoder.encode(keywords, StandardCharsets.UTF_8);
        return toComponentList(send(baseUrl, "GET", "/api/components/search?keywords=" + encoded, token, null));
    }

    public Component useComponent(String baseUrl, String token, long id) {
        return toComponent(asMap(send(baseUrl, "POST", "/api/components/" + id + "/use", token, null)));
    }

    public Component createComponent(String baseUrl, String token, ComponentRequest request) {
        return toComponent(asMap(send(baseUrl, "POST", "/api/components", token, componentRequestBody(request))));
    }

    public Component updateComponent(String baseUrl, String token, long id, ComponentRequest request) {
        return toComponent(asMap(send(baseUrl, "PUT", "/api/components/" + id, token, componentRequestBody(request))));
    }

    public void deleteComponent(String baseUrl, String token, long id) {
        send(baseUrl, "DELETE", "/api/components/" + id, token, null);
    }

    private Object send(String baseUrl, String method, String path, String token, Map<String, Object> body) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(normalizeBaseUrl(baseUrl) + path))
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", "application/json");

            if (token != null && !token.isBlank()) {
                builder.header("X-Auth-Token", token);
            }
            if (body != null) {
                builder.header("Content-Type", "application/json");
                builder.method(method, HttpRequest.BodyPublishers.ofString(Json.stringify(body)));
            } else {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            }

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body() == null ? "" : response.body().trim();
            if (response.statusCode() >= 400) {
                throw new ApiException(response.statusCode(), extractErrorMessage(responseBody));
            }
            if (responseBody.isEmpty()) {
                return null;
            }
            return Json.parse(responseBody);
        } catch (IOException e) {
            throw new ApiException("Failed to reach backend: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException("Request interrupted");
        }
    }

    private String extractErrorMessage(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "Request failed";
        }
        try {
            Object parsed = Json.parse(responseBody);
            if (parsed instanceof Map<?, ?> rawMap) {
                Map<String, Object> map = castMap(rawMap);
                Object message = map.get("message");
                if (message instanceof String string && !string.isBlank()) {
                    Object details = map.get("details");
                    if (details instanceof Map<?, ?> detailMap && !detailMap.isEmpty()) {
                        return string + " " + castMap(detailMap);
                    }
                    return string;
                }
            }
        } catch (RuntimeException ignored) {
        }
        return responseBody;
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://localhost:8080";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private Map<String, Object> componentRequestBody(ComponentRequest request) {
        return mapOf(
                "name", request.name(),
                "description", request.description(),
                "keywords", request.keywords(),
                "type", request.type() == null ? null : request.type().name(),
                "catalogueIds", request.catalogueIds()
        );
    }

    private AuthResponse toAuthResponse(Object value) {
        Map<String, Object> map = asMap(value);
        return new AuthResponse(
                asLong(map.get("userId")),
                asString(map.get("username")),
                asString(map.get("email")),
                Role.valueOf(asString(map.get("role"))),
                asString(map.get("token"))
        );
    }

    private UserProfile toUserProfile(Map<String, Object> map) {
        return new UserProfile(
                asLong(map.get("id")),
                asString(map.get("username")),
                asString(map.get("email")),
                Role.valueOf(asString(map.get("role")))
        );
    }

    private List<Catalogue> toCatalogueList(Object value) {
        List<Object> list = asList(value);
        List<Catalogue> result = new ArrayList<>();
        for (Object item : list) {
            result.add(toCatalogue(asMap(item)));
        }
        return result;
    }

    private Catalogue toCatalogue(Map<String, Object> map) {
        List<ComponentListItem> items = new ArrayList<>();
        Object components = map.get("components");
        if (components instanceof List<?> rawList) {
            for (Object item : rawList) {
                Map<String, Object> componentMap = asMap(item);
                items.add(new ComponentListItem(
                        asLong(componentMap.get("id")),
                        asString(componentMap.get("name")),
                        ComponentType.valueOf(asString(componentMap.get("type")))
                ));
            }
        }
        return new Catalogue(
                asLong(map.get("id")),
                asString(map.get("name")),
                asString(map.get("description")),
                asString(map.get("keywords")),
                asLong(map.get("ownerId")),
                asString(map.get("ownerUsername")),
                items
        );
    }

    private List<Component> toComponentList(Object value) {
        List<Object> list = asList(value);
        List<Component> result = new ArrayList<>();
        for (Object item : list) {
            result.add(toComponent(asMap(item)));
        }
        return result;
    }

    private Component toComponent(Map<String, Object> map) {
        List<Long> catalogueIds = new ArrayList<>();
        Object ids = map.get("catalogueIds");
        if (ids instanceof List<?> rawIds) {
            for (Object id : rawIds) {
                catalogueIds.add(asLong(id));
            }
        }
        return new Component(
                asLong(map.get("id")),
                asString(map.get("name")),
                asString(map.get("description")),
                asString(map.get("keywords")),
                ComponentType.valueOf(asString(map.get("type"))),
                asLong(map.get("usageCount")),
                asLong(map.get("searchHitCount")),
                asLong(map.get("searchedButNotUsedCount")),
                catalogueIds
        );
    }

    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return castMap(map);
        }
        throw new ApiException("Unexpected JSON object shape");
    }

    private List<Object> asList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            return new ArrayList<>(list);
        }
        throw new ApiException("Unexpected JSON array shape");
    }

    private Map<String, Object> castMap(Map<?, ?> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            copy.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return copy;
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(asString(value));
    }

    private Map<String, Object> mapOf(Object... entries) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            map.put(String.valueOf(entries[i]), entries[i + 1]);
        }
        return map;
    }
}
