package edu.software.project.backend.repository;

import edu.software.project.backend.entity.Component;
import edu.software.project.backend.entity.ComponentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ComponentRepository extends JpaRepository<Component, Long> {
    List<Component> findByType(ComponentType type);

    @Query("""
            select distinct c from Component c
            where lower(c.name) like lower(concat('%', :term, '%'))
               or lower(coalesce(c.description, '')) like lower(concat('%', :term, '%'))
               or lower(coalesce(c.keywords, '')) like lower(concat('%', :term, '%'))
            """)
    List<Component> searchByTerm(String term);
}
