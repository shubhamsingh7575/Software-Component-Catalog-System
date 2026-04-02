package edu.software.project.backend.controller;

import edu.software.project.backend.entity.Catalogue;
import edu.software.project.backend.entity.User;
import edu.software.project.backend.repository.CatalogueRepository;
import edu.software.project.backend.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalogue")
public class CatalogueController {
    private final CatalogueRepository catalogueRepository;
    private final UserRepository userRepository;

    public CatalogueController(CatalogueRepository catalogueRepository, UserRepository userRepository) {
        this.catalogueRepository = catalogueRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/create")
    /**
     * POST request:
     * /api/catalogue/create?userId=1&name=MyCatalogue
     */
    public Catalogue createCatalogue(@RequestParam Long userId, @RequestParam String name) {
        // Fetch user from DB
        User user = userRepository.findById(userId).orElseThrow();
        Catalogue catalogue = new Catalogue();
        catalogue.setName(name);
        catalogue.setOwner(user);
        return catalogueRepository.save(catalogue);
    }

    @GetMapping("/all")
    public List<Catalogue> getAllCatalogues() {
        return catalogueRepository.findAll();
    }
}
