package com.flipadvisor;

final class MarketItem
{
	final int id;
	final String name;
	final int limit;
	final long high;
	final long low;
	final long highTime;
	final long lowTime;
	final long hourlyVolume;
	final double hourlyAverageHigh;
	final double hourlyAverageLow;

	MarketItem(
		int id,
		String name,
		int limit,
		long high,
		long low,
		long highTime,
		long lowTime,
		long hourlyVolume,
		double hourlyAverageHigh,
		double hourlyAverageLow)
	{
		this.id = id;
		this.name = name;
		this.limit = limit;
		this.high = high;
		this.low = low;
		this.highTime = highTime;
		this.lowTime = lowTime;
		this.hourlyVolume = hourlyVolume;
		this.hourlyAverageHigh = hourlyAverageHigh;
		this.hourlyAverageLow = hourlyAverageLow;
	}
}
