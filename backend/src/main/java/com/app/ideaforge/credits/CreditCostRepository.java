package com.app.ideaforge.credits;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditCostRepository extends JpaRepository<CreditCost, String> {

	List<CreditCost> findByActiveTrue();
}
