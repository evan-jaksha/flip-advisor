package com.flipadvisor;

import java.util.Arrays;
import java.util.List;

public class MarketAnalyzerTest
{
	private static final long NOW = 1_800_000_000L;

	public static void main(String[] args)
	{
		estimatesNetProfitAfterTaxAndUsesTheLowerOfLimitAndCapital();
		rejectsStalePricesAndNonPositiveNetMargins();
		lowRiskRejectsLargeDeviationFromTheHourlyAverage();
	}

	private static void estimatesNetProfitAfterTaxAndUsesTheLowerOfLimitAndCapital()
	{
		MarketItem item = new MarketItem(100, "Test item", 20, 120, 100,
			NOW - 30, NOW - 30, 1000, 110, 90);

		List<FlipOpportunity> result = MarketAnalyzer.analyze(
			Arrays.asList(item), RiskLevel.HIGH, 30, 500, NOW);

		assertEquals(1, result.size(), "Expected one candidate");
		assertEquals(101, result.get(0).buyPrice, "Unexpected buy price");
		assertEquals(119, result.get(0).sellPrice, "Unexpected sell price");
		assertEquals(4, result.get(0).quantity, "Unexpected quantity");
		assertEquals(60, result.get(0).estimatedProfit, "Unexpected estimated profit");
	}

	private static void rejectsStalePricesAndNonPositiveNetMargins()
	{
		MarketItem stale = new MarketItem(100, "Stale", 20, 120, 100,
			NOW - 10000, NOW - 10000, 1000, 110, 90);
		MarketItem taxedOut = new MarketItem(101, "No margin", 20, 105, 100,
			NOW - 30, NOW - 30, 1000, 105, 100);

		List<FlipOpportunity> result = MarketAnalyzer.analyze(
			Arrays.asList(stale, taxedOut), RiskLevel.HIGH, 30, 500, NOW);

		assertTrue(result.isEmpty(), "Stale and taxed-out candidates must be rejected");
	}

	private static void lowRiskRejectsLargeDeviationFromTheHourlyAverage()
	{
		MarketItem volatileItem = new MarketItem(100, "Volatile", 20, 150, 100,
			NOW - 30, NOW - 30, 1000, 110, 90);

		List<FlipOpportunity> result = MarketAnalyzer.analyze(
			Arrays.asList(volatileItem), RiskLevel.LOW, 30, 5000, NOW);

		assertTrue(result.isEmpty(), "Low risk must reject large hourly deviations");
	}

	private static void assertEquals(long expected, long actual, String message)
	{
		if (expected != actual)
		{
			throw new AssertionError(message + ": expected " + expected + ", got " + actual);
		}
	}

	private static void assertEquals(int expected, int actual, String message)
	{
		if (expected != actual)
		{
			throw new AssertionError(message + ": expected " + expected + ", got " + actual);
		}
	}

	private static void assertTrue(boolean condition, String message)
	{
		if (!condition)
		{
			throw new AssertionError(message);
		}
	}
}
