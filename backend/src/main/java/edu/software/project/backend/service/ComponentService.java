package edu.software.project.backend.service;

import edu.software.project.backend.dto.ComponentRequest;
import edu.software.project.backend.dto.ComponentResponse;
import edu.software.project.backend.entity.Catalogue;
import edu.software.project.backend.entity.Component;
import edu.software.project.backend.exception.ApiException;
import edu.software.project.backend.repository.CatalogueRepository;
import edu.software.project.backend.repository.ComponentRepository;
import edu.software.project.backend.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

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

    @Transactional(readOnly = true)
    public List<ComponentResponse> getComponents(Long catalogueId) {
        return componentRepository.findByCatalogueIdOrderById(catalogueId).stream()
                .map(responseMapper::toComponentResponse)
                .toList();
    }

    @Transactional
    public List<ComponentResponse> searchComponents(Long catalogueId, String keywords) {
        List<String> terms = parseTerms(keywords);
        if (terms.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "At least one keyword is required");
        }

        return componentRepository.findByCatalogueIdOrderById(catalogueId).stream()
                .filter(component -> score(component, terms) > 0)
                .sorted(java.util.Comparator
                        .comparingInt((Component component) -> score(component, terms))
                        .reversed()
                        .thenComparing(Component::getUsageCount, java.util.Comparator.reverseOrder())
                        .thenComparing(Component::getId))
                .peek(component -> {
                    component.setSearchHitCount(component.getSearchHitCount() + 1);
                    component.setSearchedButNotUsedCount(component.getSearchedButNotUsedCount() + 1);
                })
                .map(responseMapper::toComponentResponse)
                .toList();
    }

    @Transactional
    public ComponentResponse createComponent(Long catalogueId, ComponentRequest request, AuthenticatedUser currentUser) {
        Catalogue catalogue = requireOwnedCatalogue(catalogueId, currentUser);

        Component component = new Component();
        applyComponentRequest(component, request, catalogue);
        return responseMapper.toComponentResponse(componentRepository.save(component));
    }

    @Transactional
    public ComponentResponse updateComponent(
            Long catalogueId,
            Long componentId,
            ComponentRequest request,
            AuthenticatedUser currentUser
    ) {
        Catalogue catalogue = requireOwnedCatalogue(catalogueId, currentUser);
        Component component = componentRepository.findByIdAndCatalogueId(componentId, catalogueId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Component not found in this catalogue"));
        applyComponentRequest(component, request, catalogue);
        return responseMapper.toComponentResponse(component);
    }

    @Transactional
    public void deleteComponent(Long catalogueId, Long componentId, AuthenticatedUser currentUser) {
        requireOwnedCatalogue(catalogueId, currentUser);
        Component component = componentRepository.findByIdAndCatalogueId(componentId, catalogueId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Component not found in this catalogue"));
        componentRepository.delete(component);
    }

    @Transactional
    public ComponentResponse recordUsage(Long catalogueId, Long componentId) {
        Component component = componentRepository.findByIdAndCatalogueId(componentId, catalogueId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Component not found in this catalogue"));
        component.setUsageCount(component.getUsageCount() + 1);
        if (component.getSearchedButNotUsedCount() > 0) {
            component.setSearchedButNotUsedCount(component.getSearchedButNotUsedCount() - 1);
        }
        return responseMapper.toComponentResponse(component);
    }

    private void applyComponentRequest(Component component, ComponentRequest request, Catalogue catalogue) {
        component.setName(request.name().trim());
        component.setDescription(request.description());
        component.setKeywords(request.keywords());
        component.setBody(request.body());
        component.setType(request.type());
        component.setCatalogue(catalogue);
    }

    private Catalogue requireOwnedCatalogue(Long catalogueId, AuthenticatedUser currentUser) {
        Catalogue catalogue = catalogueRepository.findById(catalogueId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Catalogue not found"));
        if (!catalogue.getOwner().getId().equals(currentUser.id())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Catalogue access denied");
        }
        return catalogue;
    }

    private List<String> parseTerms(String keywords) {
        if (keywords == null || keywords.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(keywords.split("[,\\s]+"))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .map(token -> token.toLowerCase(Locale.ROOT))
                .toList();
    }

    private int score(Component component, List<String> terms) {
        return terms.stream().mapToInt(term -> scoreTerm(component, term)).sum();
    }

    private int scoreTerm(Component component, String term) {
        return scoreText(component.getName(), term, 7)
                + scoreText(component.getKeywords(), term, 5)
                + scoreText(component.getDescription(), term, 3)
                + scoreText(component.getBody(), term, 2)
                + scoreText(component.getType() == null ? null : component.getType().name(), term, 1);
    }

    private int scoreText(String value, String term, int weight) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(term) ? weight : 0;
    }
}
