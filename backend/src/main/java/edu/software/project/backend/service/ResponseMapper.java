package edu.software.project.backend.service;

import edu.software.project.backend.dto.CatalogueResponse;
import edu.software.project.backend.dto.ComponentResponse;
import edu.software.project.backend.dto.UserProfileResponse;
import edu.software.project.backend.entity.Catalogue;
import edu.software.project.backend.entity.Component;
import edu.software.project.backend.entity.User;
import edu.software.project.backend.security.AuthenticatedUser;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class ResponseMapper {
    public UserProfileResponse toUserProfile(User user) {
        return new UserProfileResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }

    public UserProfileResponse toUserProfile(AuthenticatedUser user) {
        return new UserProfileResponse(user.id(), user.username(), user.email(), user.role());
    }

    public CatalogueResponse toCatalogueResponse(Catalogue catalogue) {
        List<ComponentResponse> components = catalogue.getComponents().stream()
                .sorted(Comparator.comparing(Component::getId))
                .map(this::toComponentResponse)
                .toList();
        return new CatalogueResponse(
                catalogue.getId(),
                catalogue.getName(),
                catalogue.getDescription(),
                catalogue.getKeywords(),
                catalogue.getOwner().getId(),
                catalogue.getOwner().getUsername(),
                components
        );
    }

    public ComponentResponse toComponentResponse(Component component) {
        return new ComponentResponse(
                component.getId(),
                component.getName(),
                component.getDescription(),
                component.getKeywords(),
                component.getBody(),
                component.getType(),
                component.getUsageCount(),
                component.getSearchHitCount(),
                component.getSearchedButNotUsedCount(),
                component.getCatalogue().getId()
        );
    }
}
