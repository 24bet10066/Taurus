package com.serviceos.auth.repository;

import com.serviceos.auth.entity.BusinessConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BusinessConfigRepository extends JpaRepository<BusinessConfig, String> {

    @Query("SELECT c FROM BusinessConfig c WHERE c.category = :category ORDER BY c.key")
    List<BusinessConfig> findByCategory(@Param("category") String category);
}
