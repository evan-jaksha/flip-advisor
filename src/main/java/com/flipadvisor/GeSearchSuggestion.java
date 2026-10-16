package com.flipadvisor;

import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;

final class GeSearchSuggestion
{
	private Widget parent;
	private Widget row;
	private Widget label;
	private int currentItemId = -1;

	void update(Widget searchResults, FlipOpportunity opportunity)
	{
		if (searchResults == null || opportunity == null || searchResults.isHidden())
		{
			hide();
			return;
		}

		if (parent != searchResults || row == null)
		{
			hide();
			parent = searchResults;
			row = searchResults.createChild(-1, WidgetType.RECTANGLE);
			row.setOriginalX(114);
			row.setOriginalY(0);
			row.setOriginalWidth(256);
			row.setOriginalHeight(32);
			row.setFilled(false);
			row.setHasListener(true);
			row.setAction(0, "Select suggested item");

			label = searchResults.createChild(-1, WidgetType.TEXT);
			label.setFontId(495);
			label.setOriginalX(120);
			label.setOriginalY(0);
			label.setOriginalWidth(240);
			label.setOriginalHeight(32);
			label.setYTextAlignment(1);
			label.setHasListener(true);
			label.setAction(0, "Select suggested item");
			currentItemId = -1;
		}

		if (currentItemId == opportunity.item.id && !row.isHidden())
		{
			return;
		}

		row.setName(opportunity.item.name);
		// The GE item-select script runs only after the player clicks this suggestion.
		row.setOnOpListener(754, opportunity.item.id, 84);
		label.setOnOpListener(754, opportunity.item.id, 84);
		row.setHidden(false);
		label.setText("Flip Advisor: " + opportunity.item.name);
		label.setTextColor(0xFFB000);
		label.setHidden(false);
		row.revalidate();
		label.revalidate();
		currentItemId = opportunity.item.id;
	}

	void hide()
	{
		if (row != null)
		{
			row.setHidden(true);
		}
		if (label != null)
		{
			label.setHidden(true);
		}
	}
}
