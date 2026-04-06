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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/components")
public class ComponentController {
    private final ComponentService componentService;

    public ComponentController(ComponentService componentService) {
        this.componentService = componentService;
    }

    @PostMapping
    public ComponentResponse createComponent(
            @Valid @RequestBody ComponentRequest request,
            @CurrentUser AuthenticatedUser authenticatedUser
    ) {
        return componentService.createComponent(request, authenticatedUser.user());
    }

    @PutMapping("/{id}")
    public ComponentResponse updateComponent(
            @PathVariable Long id,
            @Valid @RequestBody ComponentRequest request,
            @CurrentUser AuthenticatedUser authenticatedUser
    ) {
        return componentService.updateComponent(id, request, authenticatedUser.user());
    }

    @DeleteMapping("/{id}")
    public void deleteComponent(@PathVariable Long id, @CurrentUser AuthenticatedUser authenticatedUser) {
        componentService.deleteComponent(id, authenticatedUser.user());
    }

    @GetMapping
    public List<ComponentResponse> getAllComponents() {
        return componentService.getAllComponents();
    }

    @GetMapping("/{id}")
    public ComponentResponse getComponent(@PathVariable Long id) {
        return componentService.getComponent(id);
    }

    @GetMapping("/search")
    public List<ComponentResponse> searchComponents(@RequestParam String keywords) {
        return componentService.searchComponents(keywords);
    }

    @PostMapping("/{id}/use")
    public ComponentResponse recordUsage(@PathVariable Long id) {
        return componentService.recordUsage(id);
    }
}
