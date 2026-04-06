package edu.software.project.backend.service;

import edu.software.project.backend.dto.CatalogueRequest;
import edu.software.project.backend.dto.CatalogueResponse;
import edu.software.project.backend.entity.Catalogue;
import edu.software.project.backend.entity.Component;
import edu.software.project.backend.entity.User;
import edu.software.project.backend.exception.ApiException;
import edu.software.project.backend.repository.CatalogueRepository;
import edu.software.project.backend.repository.UserRepository;
import edu.software.project.backend.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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
    public List<CatalogueResponse> searchCatalogues(String keywords) {
        List<String> terms = parseTerms(keywords);
        if (terms.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "At least one keyword is required");
        }

        return catalogueRepository.findAll().stream()
                .map(catalogue -> new RankedCatalogue(catalogue, scoreCatalogue(catalogue, terms), markMatchedComponents(catalogue, terms)))
                .filter(ranked -> ranked.score() > 0)
                .sorted(Comparator
                        .comparingInt(RankedCatalogue::score)
                        .reversed()
                        .thenComparing(ranked -> ranked.catalogue().getId()))
                .map(RankedCatalogue::catalogue)
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

    private int scoreCatalogue(Catalogue catalogue, List<String> terms) {
        int score = 0;
        for (String term : terms) {
            score += scoreText(catalogue.getName(), term, 8);
            score += scoreText(catalogue.getKeywords(), term, 5);
            score += scoreText(catalogue.getDescription(), term, 3);
            for (Component component : catalogue.getComponents()) {
                score += scoreComponent(component, term);
            }
        }
        return score;
    }

    private int markMatchedComponents(Catalogue catalogue, List<String> terms) {
        int matchedCount = 0;
        for (Component component : catalogue.getComponents()) {
            boolean matched = false;
            for (String term : terms) {
                if (scoreComponent(component, term) > 0) {
                    matched = true;
                    break;
                }
            }
            if (matched) {
                component.setSearchHitCount(component.getSearchHitCount() + 1);
                component.setSearchedButNotUsedCount(component.getSearchedButNotUsedCount() + 1);
                matchedCount++;
            }
        }
        return matchedCount;
    }

    private int scoreComponent(Component component, String term) {
        return scoreText(component.getName(), term, 7)
                + scoreText(component.getKeywords(), term, 5)
                + scoreText(component.getDescription(), term, 3)
                + scoreText(component.getBody(), term, 2)
                + scoreText(component.getType() == null ? null : component.getType().name(), term, 1);
    }

    private int scoreText(String value, String term, int weight) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(term) ? weight : 0;
    }

    private record RankedCatalogue(Catalogue catalogue, int score, int matchedComponents) {
    }
}
