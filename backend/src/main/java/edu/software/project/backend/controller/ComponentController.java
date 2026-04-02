package edu.software.project.backend.controller;

import edu.software.project.backend.entity.Catalogue;
import edu.software.project.backend.entity.Component;
import edu.software.project.backend.repository.CatalogueRepository;
import edu.software.project.backend.repository.ComponentRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/component")
public class ComponentController {

    private final ComponentRepository componentRepository;
    private final CatalogueRepository catalogueRepository;

    public ComponentController(ComponentRepository componentRepository, CatalogueRepository catalogueRepository) {
        this.componentRepository = componentRepository;
        this.catalogueRepository = catalogueRepository;
    }

    @PostMapping("/create")
    /**
     * POST /api/component/create?catalogueId=1&name=ComponentName&body=content
     */
    public Component createComponent(@RequestParam Long catalogueId, @RequestParam String name, @RequestParam String body) {
        Catalogue catalogue = catalogueRepository.findById(catalogueId).orElseThrow();
        Component component = new Component();
        component.setName(name);
        component.setBody(body);
        component.setCatalogue(catalogue);

        return componentRepository.save(component);
    }
}
