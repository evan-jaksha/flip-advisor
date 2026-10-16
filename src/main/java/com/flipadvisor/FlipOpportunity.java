package com.flipadvisor;

final class FlipOpportunity
{
	final MarketItem item;
	final long buyPrice;
	final long sellPrice;
	final long quantity;
	final long estimatedProfit;
	final double netMarginPercent;
	final double hourlyDeviationPercent;
	final long hourlyVolume;

	FlipOpportunity(
		MarketItem item,
		long buyPrice,
		long sellPrice,
		long quantity,
		long estimatedProfit,
		double netMarginPercent,
		double hourlyDeviationPercent,
		long hourlyVolume)
	{
		this.item = item;
		this.buyPrice = buyPrice;
		this.sellPrice = sellPrice;
		this.quantity = quantity;
		this.estimatedProfit = estimatedProfit;
		this.netMarginPercent = netMarginPercent;
		this.hourlyDeviationPercent = hourlyDeviationPercent;
		this.hourlyVolume = hourlyVolume;
	}
}
