package com.flipadvisor;

import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;

final class GeInventory
{
	private GeInventory()
	{
	}

	static Widget findItemWidget(Client client, int itemId)
	{
		Widget inventory = client.getWidget(WidgetInfo.INVENTORY);
		if (inventory == null || inventory.getDynamicChildren() == null)
		{
			return null;
		}
		for (Widget item : inventory.getDynamicChildren())
		{
			if (item != null && item.getItemId() == itemId)
			{
				return item;
			}
		}
		return null;
	}

	static long getQuantity(Client client, int itemId)
	{
		ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
		if (inventory == null)
		{
			return 0;
		}
		long quantity = 0;
		for (Item item : inventory.getItems())
		{
			if (item != null && item.getId() == itemId)
			{
				quantity += item.getQuantity();
			}
		}
		return quantity;
	}
}
