package com.app.ideaforge.credits.dto;

import java.util.List;

import com.app.ideaforge.credits.CreditCost;

/**
 * Balance and costs in integer units (1 credit = 10 units). The UI formats;
 * the API never sends floats.
 */
public record CreditsResponse(long balanceUnits, List<CostItem> costs) {

	public record CostItem(String action, int costUnits) {

		public static CostItem from(CreditCost cost) {
			return new CostItem(cost.getAction(), cost.getCostUnits());
		}
	}

	public static CreditsResponse of(long balanceUnits, List<CreditCost> costs) {
		return new CreditsResponse(balanceUnits, costs.stream().map(CostItem::from).toList());
	}
}
