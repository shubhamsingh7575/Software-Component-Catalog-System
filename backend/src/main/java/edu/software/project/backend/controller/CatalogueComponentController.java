package edu.software.project.backend.controller;

import edu.software.project.backend.dto.ComponentRequest;
import edu.software.project.backend.dto.ComponentResponse;
import edu.software.project.backend.security.AuthenticatedUser;
import edu.software.project.backend.security.CurrentUser;
import edu.software.project.backend.service.ComponentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/catalogues/{catalogueId}/components")
public class CatalogueComponentController {
    private final ComponentService componentService;

    public CatalogueComponentController(ComponentService componentService) {
        this.componentService = componentService;
    }

    @GetMapping
    public List<ComponentResponse> getComponents(@PathVariable Long catalogueId) {
        return componentService.getComponents(catalogueId);
    }

    @PostMapping
    public ComponentResponse createComponent(
            @PathVariable Long catalogueId,
            @Valid @RequestBody ComponentRequest request,
            @CurrentUser AuthenticatedUser authenticatedUser
    ) {
        return componentService.createComponent(catalogueId, request, authenticatedUser);
    }

    @PutMapping("/{componentId}")
    public ComponentResponse updateComponent(
            @PathVariable Long catalogueId,
            @PathVariable Long componentId,
            @Valid @RequestBody ComponentRequest request,
            @CurrentUser AuthenticatedUser authenticatedUser
    ) {
        return componentService.updateComponent(catalogueId, componentId, request, authenticatedUser);
    }

    @DeleteMapping("/{componentId}")
    public void deleteComponent(
            @PathVariable Long catalogueId,
            @PathVariable Long componentId,
            @CurrentUser AuthenticatedUser authenticatedUser
    ) {
        componentService.deleteComponent(catalogueId, componentId, authenticatedUser);
    }

    @PostMapping("/{componentId}/use")
    public ComponentResponse recordUsage(@PathVariable Long catalogueId, @PathVariable Long componentId) {
        return componentService.recordUsage(catalogueId, componentId);
    }
}
