package edu.software.project.backend.service;

import edu.software.project.backend.dto.CatalogueResponse;
import edu.software.project.backend.dto.ComponentResponse;
import edu.software.project.backend.dto.ComponentSummary;
import edu.software.project.backend.dto.UserProfileResponse;
import edu.software.project.backend.entity.Catalogue;
import edu.software.project.backend.entity.User;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class ResponseMapper {
    public UserProfileResponse toUserProfile(User user) {
        return new UserProfileResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }

    public CatalogueResponse toCatalogueResponse(Catalogue catalogue) {
        List<ComponentSummary> componentSummaries = catalogue.getComponents().stream()
                .sorted(Comparator.comparing(edu.software.project.backend.entity.Component::getId))
                .map(component -> new ComponentSummary(component.getId(), component.getName(), component.getType()))
                .toList();
        return new CatalogueResponse(
                catalogue.getId(),
                catalogue.getName(),
                catalogue.getDescription(),
                catalogue.getKeywords(),
                catalogue.getOwner().getId(),
                catalogue.getOwner().getUsername(),
                componentSummaries
        );
    }

    public ComponentResponse toComponentResponse(edu.software.project.backend.entity.Component component) {
        List<Long> catalogueIds = component.getCatalogues().stream()
                .map(Catalogue::getId)
                .sorted()
                .toList();
        long searchedButNotUsedCount = Math.max(component.getSearchHitCount() - component.getUsageCount(), 0);
        return new ComponentResponse(
                component.getId(),
                component.getName(),
                component.getDescription(),
                component.getKeywords(),
                component.getType(),
                component.getUsageCount(),
                component.getSearchHitCount(),
                searchedButNotUsedCount,
                catalogueIds
        );
    }
}
