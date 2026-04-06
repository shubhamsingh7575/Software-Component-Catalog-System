package edu.software.project.backend.service;

import edu.software.project.backend.dto.CatalogueRequest;
import edu.software.project.backend.dto.CatalogueResponse;
import edu.software.project.backend.entity.Catalogue;
import edu.software.project.backend.entity.User;
import edu.software.project.backend.exception.ApiException;
import edu.software.project.backend.repository.CatalogueRepository;
import edu.software.project.backend.repository.UserRepository;
import edu.software.project.backend.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CatalogueService {
    private final CatalogueRepository catalogueRepository;
    private final UserRepository userRepository;
    private final ResponseMapper responseMapper;

    public CatalogueService(
            CatalogueRepository catalogueRepository,
            UserRepository userRepository,
            ResponseMapper responseMapper
    ) {
        this.catalogueRepository = catalogueRepository;
        this.userRepository = userRepository;
        this.responseMapper = responseMapper;
    }

    @Transactional
    public CatalogueResponse createCatalogue(CatalogueRequest request, AuthenticatedUser owner) {
        Catalogue catalogue = new Catalogue();
        catalogue.setName(request.name().trim());
        catalogue.setDescription(request.description());
        catalogue.setKeywords(request.keywords());
        catalogue.setOwner(requireUser(owner.id()));
        return responseMapper.toCatalogueResponse(catalogueRepository.save(catalogue));
    }

    @Transactional(readOnly = true)
    public List<CatalogueResponse> getAllCatalogues() {
        return catalogueRepository.findAll().stream()
                .map(responseMapper::toCatalogueResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogueResponse> getCurrentUserCatalogues(AuthenticatedUser owner) {
        return catalogueRepository.findByOwnerId(owner.id()).stream()
                .map(responseMapper::toCatalogueResponse)
                .toList();
    }

    @Transactional
    public CatalogueResponse updateCatalogue(Long id, CatalogueRequest request, AuthenticatedUser currentUser) {
        Catalogue catalogue = getOwnedCatalogue(id, currentUser);
        catalogue.setName(request.name().trim());
        catalogue.setDescription(request.description());
        catalogue.setKeywords(request.keywords());
        return responseMapper.toCatalogueResponse(catalogue);
    }

    @Transactional
    public void deleteCatalogue(Long id, AuthenticatedUser currentUser) {
        Catalogue catalogue = getOwnedCatalogue(id, currentUser);
        catalogueRepository.delete(catalogue);
    }

    @Transactional(readOnly = true)
    public Catalogue getOwnedCatalogue(Long id, AuthenticatedUser currentUser) {
        Catalogue catalogue = catalogueRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Catalogue not found"));
        if (!catalogue.getOwner().getId().equals(currentUser.id())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Catalogue access denied");
        }
        return catalogue;
    }

    private User requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Authenticated user no longer exists"));
    }
}
