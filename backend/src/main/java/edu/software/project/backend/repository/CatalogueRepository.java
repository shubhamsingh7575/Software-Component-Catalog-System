package edu.software.project.backend.repository;

import edu.software.project.backend.entity.Catalogue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CatalogueRepository extends JpaRepository<Catalogue, Long> {
    List<Catalogue> findByOwnerId(Long ownerId);
}
