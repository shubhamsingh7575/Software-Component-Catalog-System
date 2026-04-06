package edu.software.project.backend.service;

import edu.software.project.backend.dto.ComponentRequest;
import edu.software.project.backend.dto.ComponentResponse;
import edu.software.project.backend.entity.Catalogue;
import edu.software.project.backend.entity.Component;
import edu.software.project.backend.entity.Role;
import edu.software.project.backend.exception.ApiException;
import edu.software.project.backend.repository.CatalogueRepository;
import edu.software.project.backend.repository.ComponentRepository;
import edu.software.project.backend.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ComponentService {
    private final ComponentRepository componentRepository;
    private final CatalogueRepository catalogueRepository;
    private final ResponseMapper responseMapper;

    public ComponentService(
            ComponentRepository componentRepository,
            CatalogueRepository catalogueRepository,
            ResponseMapper responseMapper
    ) {
        this.componentRepository = componentRepository;
        this.catalogueRepository = catalogueRepository;
        this.responseMapper = responseMapper;
    }

    @Transactional
    public ComponentResponse createComponent(ComponentRequest request, AuthenticatedUser currentUser) {
        requireAdmin(currentUser);
        Component component = new Component();
        applyComponentRequest(component, request);
        return responseMapper.toComponentResponse(componentRepository.save(component));
    }

    @Transactional
    public ComponentResponse updateComponent(Long id, ComponentRequest request, AuthenticatedUser currentUser) {
        requireAdmin(currentUser);
        Component component = componentRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Component not found"));
        applyComponentRequest(component, request);
        return responseMapper.toComponentResponse(component);
    }

    @Transactional
    public void deleteComponent(Long id, AuthenticatedUser currentUser) {
        requireAdmin(currentUser);
        Component component = componentRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Component not found"));
        componentRepository.delete(component);
    }

    @Transactional(readOnly = true)
    public List<ComponentResponse> getAllComponents() {
        return componentRepository.findAll().stream()
                .sorted(Comparator.comparing(Component::getId))
                .map(responseMapper::toComponentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ComponentResponse getComponent(Long id) {
        Component component = componentRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Component not found"));
        return responseMapper.toComponentResponse(component);
    }

    @Transactional
    public List<ComponentResponse> searchComponents(String keywords) {
        List<String> terms = parseTerms(keywords);
        if (terms.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "At least one keyword is required");
        }

        List<Component> ranked = rankComponents(terms);
        ranked.forEach(component -> {
            component.setSearchHitCount(component.getSearchHitCount() + 1);
            component.setSearchedButNotUsedCount(component.getSearchedButNotUsedCount() + 1);
        });

        return ranked.stream()
                .map(responseMapper::toComponentResponse)
                .toList();
    }

    @Transactional
    public ComponentResponse recordUsage(Long id) {
        Component component = componentRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Component not found"));
        component.setUsageCount(component.getUsageCount() + 1);
        if (component.getSearchedButNotUsedCount() > 0) {
            component.setSearchedButNotUsedCount(component.getSearchedButNotUsedCount() - 1);
        }
        return responseMapper.toComponentResponse(component);
    }

    private void applyComponentRequest(Component component, ComponentRequest request) {
        component.setName(request.name().trim());
        component.setDescription(request.description());
        component.setKeywords(request.keywords());
        component.setType(request.type());
        component.setCatalogues(resolveCatalogues(request.catalogueIds()));
    }

    private Set<Catalogue> resolveCatalogues(Set<Long> catalogueIds) {
        if (catalogueIds == null || catalogueIds.isEmpty()) {
            return new LinkedHashSet<>();
        }

        List<Catalogue> catalogues = catalogueRepository.findAllById(catalogueIds);
        if (catalogues.size() != catalogueIds.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "One or more catalogue ids are invalid");
        }
        return new LinkedHashSet<>(catalogues);
    }

    private List<String> parseTerms(String keywords) {
        if (keywords == null || keywords.isBlank()) {
            return List.of();
        }

        String[] rawTerms = keywords.split("[,\\s]+");
        List<String> terms = new ArrayList<>();
        for (String rawTerm : rawTerms) {
            if (!rawTerm.isBlank()) {
                terms.add(rawTerm.toLowerCase(Locale.ROOT));
            }
        }
        return terms;
    }

    private List<Component> rankComponents(List<String> terms) {
        return componentRepository.findAll().stream()
                .filter(component -> matches(component, terms))
                .sorted(Comparator
                        .comparingInt((Component component) -> score(component, terms))
                        .reversed()
                        .thenComparing(Component::getUsageCount, Comparator.reverseOrder())
                        .thenComparing(Component::getId))
                .toList();
    }

    private boolean matches(Component component, List<String> terms) {
        return terms.stream().anyMatch(term -> scoreTerm(component, term) > 0);
    }

    private int score(Component component, List<String> terms) {
        return terms.stream().mapToInt(term -> scoreTerm(component, term)).sum();
    }

    private int scoreTerm(Component component, String term) {
        int score = 0;
        if (containsIgnoreCase(component.getName(), term)) {
            score += 5;
        }
        if (containsIgnoreCase(component.getKeywords(), term)) {
            score += 3;
        }
        if (containsIgnoreCase(component.getDescription(), term)) {
            score += 1;
        }
        return score;
    }

    private boolean containsIgnoreCase(String value, String term) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(term);
    }

    private void requireAdmin(AuthenticatedUser currentUser) {
        if (currentUser.role() != Role.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Admin role required");
        }
    }
}
