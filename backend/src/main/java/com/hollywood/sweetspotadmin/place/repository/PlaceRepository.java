package com.hollywood.sweetspotadmin.place.repository;

import com.hollywood.sweetspotadmin.place.model.Place;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Stream;

@Repository
public interface PlaceRepository extends JpaRepository<Place, Long>, JpaSpecificationExecutor<Place> {

    @Query("SELECT DISTINCT p.mainCategory FROM Place p ORDER BY p.mainCategory ASC")
    List<String> findDistinctMainCategories();

    @Query(value = "SELECT p FROM Place p WHERE p.geom IS NULL AND p.epsg5174x IS NULL",
           countQuery = "SELECT COUNT(p) FROM Place p WHERE p.geom IS NULL AND p.epsg5174x IS NULL")
    Page<Place> findByGeomIsNullAndEpsg5174xIsNull(Pageable pageable);

    // Analysis Queries
    @Query("SELECT p.mainCategory, COUNT(p) FROM Place p GROUP BY p.mainCategory")
    List<Object[]> findMainCategoryFrequency();

    @Query("SELECT p.subCategory, COUNT(p) FROM Place p GROUP BY p.subCategory")
    List<Object[]> findSubCategoryFrequency();

    @Query("SELECT name FROM Place")
    @QueryHints(@QueryHint(name = "org.hibernate.fetchSize", value = "1000"))
    Stream<String> streamAllNames();

    @Query("SELECT name FROM Place p WHERE p.mainCategory = :mainCategory")
    @QueryHints(@QueryHint(name = "org.hibernate.fetchSize", value = "1000"))
    Stream<String> streamNamesByMainCategory(String mainCategory);
}