package edu.software.project.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "components")
public class Component {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 3000)
    private String description;

    @Column(length = 1000)
    private String keywords;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComponentType type;

    @Column(nullable = false)
    private long usageCount = 0;

    @Column(nullable = false)
    private long searchHitCount = 0;

    @ManyToMany(mappedBy = "components")
    private Set<Catalogue> catalogues = new LinkedHashSet<>();

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public ComponentType getType() {
        return type;
    }

    public void setType(ComponentType type) {
        this.type = type;
    }

    public long getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(long usageCount) {
        this.usageCount = usageCount;
    }

    public long getSearchHitCount() {
        return searchHitCount;
    }

    public void setSearchHitCount(long searchHitCount) {
        this.searchHitCount = searchHitCount;
    }

    public Set<Catalogue> getCatalogues() {
        return catalogues;
    }

    public void setCatalogues(Set<Catalogue> catalogues) {
        this.catalogues = catalogues;
    }
}
