package com.flipadvisor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import net.runelite.api.Client;
import net.runelite.api.VarPlayer;
import net.runelite.api.Varbits;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

final class FlipAdvisorOverlay extends Overlay
{
	private static final int GE_SETUP_PRICE_VARP = 5753; // Internal GE price varp; verify when RuneLite updates.
	private static final Color HIGHLIGHT = new Color(255, 170, 0, 230);
	private final Client client;
	private volatile FlipOpportunity selected;

	FlipAdvisorOverlay(Client client)
	{
		this.client = client;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
	}

	void setSelected(FlipOpportunity selected)
	{
		this.selected = selected;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		FlipOpportunity opportunity = selected;
		if (opportunity == null)
		{
			return null;
		}

		Widget ge = client.getWidget(InterfaceID.GE_OFFERS, 7);
		if (ge == null || ge.isHidden())
		{
			return null;
		}

		Widget offerContainer = client.getWidget(InterfaceID.GE_OFFERS, 26);
		Widget confirm = offerContainer == null ? null : offerContainer.getChild(58);
		if (confirm != null && !confirm.isHidden())
		{
			highlightOfferControl(graphics, opportunity, offerContainer);
		}
		else
		{
			Widget inventoryItem = GeInventory.findItemWidget(client, opportunity.item.id);
			if (inventoryItem != null)
			{
				drawHighlight(graphics, inventoryItem);
			}
			else
			{
				highlightBuySlot(graphics);
			}
		}
		return null;
	}

	private void highlightBuySlot(Graphics2D graphics)
	{
		for (int slot = 0; slot < 8; slot++)
		{
			Widget slotWidget = client.getWidget(InterfaceID.GE_OFFERS, 7 + slot);
			Widget action = slotWidget == null ? null : slotWidget.getChild(0);
			if (action != null && !action.isHidden()
				&& action.getText() != null && action.getText().toLowerCase().contains("buy"))
			{
				drawHighlight(graphics, action);
				return;
			}
		}
	}

	private void highlightOfferControl(Graphics2D graphics, FlipOpportunity opportunity, Widget offerContainer)
	{
		int currentItem = client.getVarpValue(VarPlayer.CURRENT_GE_ITEM);
		boolean selling = client.getVarbitValue(Varbits.GE_OFFER_CREATION_TYPE) == 1;
		if (selling && currentItem != opportunity.item.id)
		{
			drawHighlight(graphics, GeInventory.findItemWidget(client, opportunity.item.id));
			return;
		}
		if (currentItem != opportunity.item.id)
		{
			return;
		}

		long currentPrice = client.getVarpLongValue(GE_SETUP_PRICE_VARP);
		int currentQuantity = client.getVarbitValue(VarbitID.GE_NEWOFFER_QUANTITY);
		Widget target;
		long targetPrice = selling ? opportunity.sellPrice : opportunity.buyPrice;
		long targetQuantity = selling ? GeInventory.getQuantity(client, opportunity.item.id) : opportunity.quantity;
		if (currentPrice != targetPrice)
		{
			target = offerContainer.getChild(54);
		}
		else if (currentQuantity != targetQuantity)
		{
			target = offerContainer.getChild(51);
		}
		else
		{
			target = offerContainer.getChild(58);
		}
		drawHighlight(graphics, target);
	}

	private void drawHighlight(Graphics2D graphics, Widget widget)
	{
		if (widget == null || widget.isHidden())
		{
			return;
		}
		Rectangle bounds = widget.getBounds();
		if (bounds == null || bounds.isEmpty())
		{
			return;
		}
		Graphics2D g = (Graphics2D) graphics.create();
		try
		{
			g.setColor(HIGHLIGHT);
			g.setStroke(new BasicStroke(2f));
			g.drawRoundRect(bounds.x - 2, bounds.y - 2, bounds.width + 4, bounds.height + 4, 6, 6);
		}
		finally
		{
			g.dispose();
		}
	}
}
