package com.flipadvisor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class MarketAnalyzer
{
	private static final long GE_TAX_CAP = 5_000_000L;
	private static final long GE_TAX_DIVISOR = 50L;

	private MarketAnalyzer()
	{
	}

	static List<FlipOpportunity> analyze(
		List<MarketItem> items,
		RiskLevel risk,
		int holdingWindowMinutes,
		long capitalGp,
		long nowEpochSeconds)
	{
		List<FlipOpportunity> opportunities = new ArrayList<>();
		long maxPriceAge = Math.max(1800L, holdingWindowMinutes * 120L);
		double minHourlyVolume = minimumVolume(holdingWindowMinutes) * risk.getVolumeMultiplier();

		for (MarketItem item : items)
		{
			if (item.limit <= 0 || item.high <= 0 || item.low <= 0 || item.high <= item.low
				|| item.hourlyAverageHigh <= 0 || item.hourlyAverageLow <= 0)
			{
				continue;
			}
			if (item.highTime <= 0 || item.lowTime <= 0
				|| nowEpochSeconds - item.highTime > maxPriceAge
				|| nowEpochSeconds - item.lowTime > maxPriceAge
				|| item.hourlyVolume < minHourlyVolume)
			{
				continue;
			}

			long buyPrice = item.low + 1;
			long sellPrice = item.high - 1;
			long tax = Math.min((sellPrice / GE_TAX_DIVISOR)
				+ (sellPrice % GE_TAX_DIVISOR == 0 ? 0 : 1), GE_TAX_CAP);
			long profitPerItem = sellPrice - tax - buyPrice;
			if (profitPerItem <= 0)
			{
				continue;
			}

			double marginPercent = (profitPerItem * 100.0) / buyPrice;
			double deviationPercent = calculateHourlyDeviation(item);
			if (marginPercent < risk.getMinNetMarginPercent()
				|| Math.abs(deviationPercent) > risk.getMaxDeviationPercent())
			{
				continue;
			}

			long affordableQuantity = capitalGp / buyPrice;
			long quantity = Math.min(item.limit, affordableQuantity);
			if (quantity <= 0 || profitPerItem > Long.MAX_VALUE / quantity)
			{
				continue;
			}

			opportunities.add(new FlipOpportunity(
				item,
				buyPrice,
				sellPrice,
				quantity,
				profitPerItem * quantity,
				marginPercent,
				deviationPercent,
				item.hourlyVolume));
		}

		opportunities.sort(Comparator
			.comparingLong((FlipOpportunity opportunity) -> opportunity.estimatedProfit)
			.reversed()
			.thenComparing(Comparator.comparingLong((FlipOpportunity opportunity) -> opportunity.hourlyVolume).reversed()));
		return opportunities;
	}

	private static long minimumVolume(int holdingWindowMinutes)
	{
		if (holdingWindowMinutes <= 5)
		{
			return 25;
		}
		if (holdingWindowMinutes <= 30)
		{
			return 10;
		}
		if (holdingWindowMinutes <= 120)
		{
			return 5;
		}
		return 1;
	}

	private static double calculateHourlyDeviation(MarketItem item)
	{
		if (item.hourlyAverageHigh <= 0 || item.hourlyAverageLow <= 0)
		{
			return 0;
		}

		double latestMid = (item.high + item.low) / 2.0;
		double hourlyMid = (item.hourlyAverageHigh + item.hourlyAverageLow) / 2.0;
		return ((latestMid - hourlyMid) * 100.0) / hourlyMid;
	}
}
