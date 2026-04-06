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

import java.util.List;

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
    public ComponentResponse createComponent(Long catalogueId, ComponentRequest request, AuthenticatedUser currentUser) {
        requireAdmin(currentUser);
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
        requireAdmin(currentUser);
        Catalogue catalogue = requireOwnedCatalogue(catalogueId, currentUser);
        Component component = componentRepository.findByIdAndCatalogueId(componentId, catalogueId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Component not found in this catalogue"));
        applyComponentRequest(component, request, catalogue);
        return responseMapper.toComponentResponse(component);
    }

    @Transactional
    public void deleteComponent(Long catalogueId, Long componentId, AuthenticatedUser currentUser) {
        requireAdmin(currentUser);
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

    private void requireAdmin(AuthenticatedUser currentUser) {
        if (currentUser.role() != Role.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Admin role required");
        }
    }
}
