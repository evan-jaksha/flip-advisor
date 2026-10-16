package com.flipadvisor;

enum RiskLevel
{
	LOW("Low", 1.5, 2.0, 3.0),
	MEDIUM("Medium", 0.8, 1.0, 10.0),
	HIGH("High", 0.2, 0.5, Double.POSITIVE_INFINITY);

	private final String label;
	private final double minNetMarginPercent;
	private final double volumeMultiplier;
	private final double maxDeviationPercent;

	RiskLevel(String label, double minNetMarginPercent, double volumeMultiplier, double maxDeviationPercent)
	{
		this.label = label;
		this.minNetMarginPercent = minNetMarginPercent;
		this.volumeMultiplier = volumeMultiplier;
		this.maxDeviationPercent = maxDeviationPercent;
	}

	double getMinNetMarginPercent()
	{
		return minNetMarginPercent;
	}

	double getVolumeMultiplier()
	{
		return volumeMultiplier;
	}

	double getMaxDeviationPercent()
	{
		return maxDeviationPercent;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
