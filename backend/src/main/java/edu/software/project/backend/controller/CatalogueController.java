package edu.software.project.backend.controller;

import edu.software.project.backend.dto.CatalogueRequest;
import edu.software.project.backend.dto.CatalogueResponse;
import edu.software.project.backend.security.AuthenticatedUser;
import edu.software.project.backend.security.CurrentUser;
import edu.software.project.backend.service.CatalogueService;
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
@RequestMapping("/api/catalogues")
public class CatalogueController {
    private final CatalogueService catalogueService;

    public CatalogueController(CatalogueService catalogueService) {
        this.catalogueService = catalogueService;
    }

    @PostMapping
    public CatalogueResponse createCatalogue(
            @Valid @RequestBody CatalogueRequest request,
            @CurrentUser AuthenticatedUser authenticatedUser
    ) {
        return catalogueService.createCatalogue(request, authenticatedUser.user());
    }

    @GetMapping
    public List<CatalogueResponse> getAllCatalogues() {
        return catalogueService.getAllCatalogues();
    }

    @GetMapping("/mine")
    public List<CatalogueResponse> getMyCatalogues(@CurrentUser AuthenticatedUser authenticatedUser) {
        return catalogueService.getCurrentUserCatalogues(authenticatedUser.user());
    }

    @PutMapping("/{id}")
    public CatalogueResponse updateCatalogue(
            @PathVariable Long id,
            @Valid @RequestBody CatalogueRequest request,
            @CurrentUser AuthenticatedUser authenticatedUser
    ) {
        return catalogueService.updateCatalogue(id, request, authenticatedUser.user());
    }

    @DeleteMapping("/{id}")
    public void deleteCatalogue(@PathVariable Long id, @CurrentUser AuthenticatedUser authenticatedUser) {
        catalogueService.deleteCatalogue(id, authenticatedUser.user());
    }
}
