package com.app.ideaforge.research;

import java.time.LocalDate;
import java.util.List;

/**
 * Optional trend signal. Free Google Trends access is unreliable, so the
 * default is a no-op and every implementation must fail gracefully.
 */
public interface TrendsProvider {

	boolean isEnabled();

	List<TrendPoint> interest(String query, String region);

	record TrendPoint(LocalDate date, int value) {
	}
}
