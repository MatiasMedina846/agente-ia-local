package com.agente.agente_ia_local.repository;

import com.agente.agente_ia_local.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByTenantIdAndActiveTrue(Long tenantId);

    Optional<Product> findByTenantIdAndSku(Long tenantId, String sku);

    @Query("SELECT p FROM Product p WHERE p.tenantId = :tenantId AND p.active = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.tags) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Product> searchByKeyword(@Param("tenantId") Long tenantId, @Param("query") String query);

    List<Product> findByTenantIdAndActiveTrueAndCategoriesContainingIgnoreCase(Long tenantId, String category);
}
