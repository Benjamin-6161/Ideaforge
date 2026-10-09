package com.app.ideaforge.credits;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditPackageRepository extends JpaRepository<CreditPackage, UUID> {

	List<CreditPackage> findByActiveTrueOrderBySortOrderAsc();
}
