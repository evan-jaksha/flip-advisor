package com.flipadvisor;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("flipadvisor")
public interface FlipAdvisorConfig extends Config
{
	@ConfigItem(
		keyName = "capitalGp",
		name = "Capital to allocate",
		description = "Maximum GP used to estimate the size and profit of one suggested buy offer"
	)
	default int capitalGp()
	{
		return 1000000;
	}
}
