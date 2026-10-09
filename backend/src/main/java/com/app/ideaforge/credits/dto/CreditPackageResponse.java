package com.app.ideaforge.credits.dto;

import java.util.UUID;

import com.app.ideaforge.credits.CreditPackage;

public record CreditPackageResponse(
		UUID id,
		String code,
		String name,
		int creditsUnits,
		int priceCents,
		String currency) {

	public static CreditPackageResponse from(CreditPackage creditPackage) {
		return new CreditPackageResponse(
				creditPackage.getId(),
				creditPackage.getCode(),
				creditPackage.getName(),
				creditPackage.getCreditsUnits(),
				creditPackage.getPriceCents(),
				creditPackage.getCurrency());
	}
}
