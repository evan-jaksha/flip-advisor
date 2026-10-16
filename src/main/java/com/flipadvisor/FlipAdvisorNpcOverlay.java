package com.flipadvisor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

final class FlipAdvisorNpcOverlay extends Overlay
{
	private static final Color HIGHLIGHT = new Color(255, 170, 0, 230);
	private final Client client;
	private final ModelOutlineRenderer outlineRenderer;
	private volatile boolean active;

	FlipAdvisorNpcOverlay(Client client, ModelOutlineRenderer outlineRenderer)
	{
		this.client = client;
		this.outlineRenderer = outlineRenderer;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(Overlay.PRIORITY_HIGH);
	}

	void setActive(boolean active)
	{
		this.active = active;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!active)
		{
			return null;
		}
		net.runelite.api.widgets.Widget ge = client.getWidget(InterfaceID.GE_OFFERS, 7);
		if (ge != null && !ge.isHidden())
		{
			return null;
		}
		Player player = client.getLocalPlayer();
		if (player == null || player.getWorldLocation() == null)
		{
			return null;
		}

		NPC closest = null;
		int closestDistance = Integer.MAX_VALUE;
		for (NPC npc : client.getNpcs())
		{
			if (npc == null || !"Grand Exchange Clerk".equals(npc.getName())
				|| npc.getWorldLocation() == null)
			{
				continue;
			}
			int distance = npc.getWorldLocation().distanceTo(player.getWorldLocation());
			if (distance < closestDistance)
			{
				closest = npc;
				closestDistance = distance;
			}
		}
		if (closest != null && closestDistance <= 12)
		{
			outlineRenderer.drawOutline(closest, 2, HIGHLIGHT, 4);
		}
		return null;
	}

}
