package com.flipadvisor;

import com.google.gson.JsonParseException;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.VarPlayer;
import net.runelite.api.Varbits;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;
import okhttp3.OkHttpClient;

@PluginDescriptor(
	name = "Flip Advisor",
	description = "Read-only market research with explicit, user-triggered GE setup assistance"
)
public class FlipAdvisorPlugin extends Plugin
{
	private static final Logger LOG = Logger.getLogger(FlipAdvisorPlugin.class.getName());
	private static final int REFRESH_MINUTES = 5;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ModelOutlineRenderer outlineRenderer;

	@Inject
	private OkHttpClient httpClient;

	@Inject
	private FlipAdvisorConfig config;

	private FlipAdvisorPanel panel;
	private FlipAdvisorOverlay overlay;
	private FlipAdvisorNpcOverlay npcOverlay;
	private NavigationButton navigationButton;
	private MarketDataService marketDataService;
	private ScheduledExecutorService executor;
	private final GeSearchSuggestion geSearchSuggestion = new GeSearchSuggestion();
	private volatile List<MarketItem> marketItems = Collections.emptyList();
	private volatile FlipOpportunity selected;
	private volatile RiskLevel risk = RiskLevel.MEDIUM;
	private volatile int holdingWindowMinutes = 30;

	@Override
	protected void startUp()
	{
		marketDataService = new MarketDataService(httpClient);
		panel = new FlipAdvisorPanel(
			this::refreshMarketData,
			this::selectOpportunity,
			() -> setNumericInput(true),
			() -> setNumericInput(false),
			this::filtersChanged);
		overlay = new FlipAdvisorOverlay(client);
		overlayManager.add(overlay);
		npcOverlay = new FlipAdvisorNpcOverlay(client, outlineRenderer);
		overlayManager.add(npcOverlay);
		navigationButton = NavigationButton.builder()
			.tooltip("Flip Advisor")
			.icon(createIcon())
			.priority(6)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navigationButton);

		executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
			Thread thread = new Thread(runnable, "flip-advisor-market-refresh");
			thread.setDaemon(true);
			return thread;
		});
		executor.scheduleAtFixedRate(this::loadMarketData, 0, REFRESH_MINUTES, TimeUnit.MINUTES);
	}

	@Override
	protected void shutDown()
	{
		if (executor != null)
		{
			executor.shutdownNow();
			executor = null;
		}
		clientToolbar.removeNavigation(navigationButton);
		overlayManager.remove(overlay);
		overlayManager.remove(npcOverlay);
		clientThread.invokeLater(geSearchSuggestion::hide);
	}

	@Provides
	FlipAdvisorConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(FlipAdvisorConfig.class);
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		FlipOpportunity current = selected;
		Widget searchResults = client.getWidget(ComponentID.CHATBOX_GE_SEARCH_RESULTS);
		geSearchSuggestion.update(searchResults, current);
		if (client.getTickCount() % 5 == 0)
		{
			updateActiveOfferAdvice();
		}
	}

	private void refreshMarketData()
	{
		ScheduledExecutorService currentExecutor = executor;
		if (currentExecutor != null && !currentExecutor.isShutdown())
		{
			currentExecutor.execute(this::loadMarketData);
			panel.setStatus("Refreshing market data...");
		}
	}

	private void loadMarketData()
	{
		try
		{
			marketItems = marketDataService.loadMarketData();
			rebuildOpportunities();
			panel.setStatus("Updated " + marketItems.size() + " items at "
				+ java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
					.withZone(java.time.ZoneId.systemDefault())
					.format(Instant.now()));
		}
		catch (IOException | JsonParseException | IllegalStateException ex)
		{
			LOG.log(Level.SEVERE, "Unable to refresh market data from the OSRS Wiki prices API", ex);
			panel.setStatus("Market data unavailable: " + ex.getMessage());
		}
	}

	private void filtersChanged(RiskLevel selectedRisk, int selectedWindowMinutes)
	{
		risk = selectedRisk;
		holdingWindowMinutes = selectedWindowMinutes;
		rebuildOpportunities();
	}

	private void rebuildOpportunities()
	{
		List<FlipOpportunity> refreshed = MarketAnalyzer.analyze(
			marketItems,
			risk,
			holdingWindowMinutes,
			Math.max(1, config.capitalGp()),
			Instant.now().getEpochSecond());
		panel.showCandidates(refreshed);

		FlipOpportunity current = selected;
		if (current != null)
		{
			FlipOpportunity replacement = refreshed.stream()
				.filter(item -> item.item.id == current.item.id)
				.findFirst()
				.orElse(null);
			selected = replacement;
			overlay.setSelected(replacement);
			npcOverlay.setActive(replacement != null);
			panel.setSelected(replacement);
		}
	}

	private void updateActiveOfferAdvice()
	{
		Map<Integer, MarketItem> byId = new HashMap<>();
		for (MarketItem item : marketItems)
		{
			byId.put(item.id, item);
		}

		List<ActiveOfferAdvice> advice = new ArrayList<>();
		GrandExchangeOffer[] offers = client.getGrandExchangeOffers();
		if (offers != null)
		{
			for (int slot = 0; slot < offers.length; slot++)
			{
				GrandExchangeOffer offer = offers[slot];
				if (offer == null || (offer.getState() != GrandExchangeOfferState.BUYING
					&& offer.getState() != GrandExchangeOfferState.SELLING))
				{
					continue;
				}

				MarketItem item = byId.get(offer.getItemId());
				if (item == null || item.high <= 0 || item.low <= 0
					|| item.highTime <= 0 || item.lowTime <= 0
					|| Instant.now().getEpochSecond() - item.highTime > TimeUnit.HOURS.toSeconds(2)
					|| Instant.now().getEpochSecond() - item.lowTime > TimeUnit.HOURS.toSeconds(2))
				{
					continue;
				}

				boolean buying = offer.getState() == GrandExchangeOfferState.BUYING;
				long comparisonPrice = buying ? item.low + 1 : Math.max(1, item.high - 1);
				String action = buying ? "BUY" : "SELL";
				String detail;
				if (buying && offer.getPrice() < comparisonPrice)
				{
					detail = "Below the recent sell range; review a reprice near "
						+ comparisonPrice + " gp.";
				}
				else if (buying && offer.getPrice() > item.high)
				{
					detail = "Above the latest instant-buy price; review for overpay risk.";
				}
				else if (!buying && offer.getPrice() > comparisonPrice)
				{
					detail = "Above the recent buy range; review a reprice near "
						+ comparisonPrice + " gp.";
				}
				else if (!buying && offer.getPrice() < item.low)
				{
					detail = "Below the latest instant-sell price; review for underpricing.";
				}
				else
				{
					detail = "Offer price is near the latest market range; keep monitoring.";
				}
				advice.add(new ActiveOfferAdvice(
					"Slot " + (slot + 1) + " " + action + " " + item.name + ": " + detail));
			}
		}
		panel.showActiveOffers(advice);
	}

	private void selectOpportunity(FlipOpportunity opportunity)
	{
		selected = opportunity;
		overlay.setSelected(opportunity);
		npcOverlay.setActive(true);
		panel.setSelected(opportunity);
		panel.setStatus("Selected " + opportunity.item.name
			+ ". Open the GE search and click the suggested row; review every value before confirming.");
	}

	private void setNumericInput(boolean price)
	{
		FlipOpportunity opportunity = selected;
		if (opportunity == null)
		{
			panel.setStatus("Select a suggestion first.");
			return;
		}

		clientThread.invokeLater(() -> {
			int currentItem = client.getVarpValue(VarPlayer.CURRENT_GE_ITEM);
			Widget title = client.getWidget(ComponentID.CHATBOX_TITLE);
			Widget input = client.getWidget(ComponentID.CHATBOX_FULL_INPUT);
			if (currentItem != opportunity.item.id || title == null || input == null)
			{
				panel.setStatus("Open the matching item's numeric price or amount input first.");
				return;
			}

			String prompt = title.getText();
			int inputMode = client.getVarcIntValue(VarClientID.MESLAYERMODE);
			if (inputMode != 7 && inputMode != 30)
			{
				panel.setStatus("Open the game's numeric input before inserting a value.");
				return;
			}
			boolean isPricePrompt = prompt != null && prompt.toLowerCase().contains("price");
			boolean isQuantityPrompt = prompt != null && prompt.toLowerCase().contains("how many");
			if ((price && !isPricePrompt) || (!price && !isQuantityPrompt))
			{
				panel.setStatus(price ? "Open the price input before inserting the suggested price."
					: "Open the amount input before inserting the suggested amount.");
				return;
			}

			boolean selling = client.getVarbitValue(Varbits.GE_OFFER_CREATION_TYPE) == 1;
			long value;
			if (price)
			{
				value = selling ? opportunity.sellPrice : opportunity.buyPrice;
			}
			else if (selling)
			{
				value = GeInventory.getQuantity(client, opportunity.item.id);
				if (value <= 0)
				{
					panel.setStatus("The suggested item is not in your inventory.");
					return;
				}
			}
			else
			{
				value = opportunity.quantity;
			}
			input.setText(Long.toString(value));
			panel.setStatus("Inserted " + value + ". Review it and submit manually in game.");
		});
	}

	private static BufferedImage createIcon()
	{
		BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
		java.awt.Graphics2D graphics = image.createGraphics();
		try
		{
			graphics.setColor(new java.awt.Color(35, 35, 35));
			graphics.fillRoundRect(1, 1, 30, 30, 8, 8);
			graphics.setColor(new java.awt.Color(255, 170, 0));
			graphics.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 19));
			graphics.drawString("F", 10, 23);
		}
		finally
		{
			graphics.dispose();
		}
		return image;
	}
}
