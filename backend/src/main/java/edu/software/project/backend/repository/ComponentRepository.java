package edu.software.project.backend.repository;

import edu.software.project.backend.entity.Component;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ComponentRepository extends JpaRepository<Component, Long> {
    List<Component> findByCatalogueIdOrderById(Long catalogueId);

    Optional<Component> findByIdAndCatalogueId(Long id, Long catalogueId);
}
