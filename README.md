# Flip Advisor

Flip Advisor is a RuneLite plugin for Grand Exchange market research and manually assisted offer setup.

## Features

- Reads bulk latest and hourly prices plus item limits from the OSRS Wiki real-time prices API.
- Ranks candidate buy/sell spreads using estimated GE tax, hourly volume, price freshness, risk settings, and a holding-time filter.
- Shows suggested buy prices and quantities, with inventory-aware guidance for manually selling an item already owned.
- Adds a clickable suggested-item row to GE search results and visual overlays for the clerk, inventory items, and relevant GE controls.
- Provides read-only guidance on active offers versus recent market prices.
- Sidebar buttons can insert suggested values into an already-open numeric input. You must review and submit every value and confirm every offer yourself.

## Safety and limitations

This plugin does not click controls, synthesize keypresses, submit or cancel offers, or confirm trades. It does not send account or offer data to a remote service. The only network requests are bulk OSRS Wiki market-data requests every five minutes, with a descriptive User-Agent.

Prices may be sparse or aggregated; suggestions and profit estimates are not guarantees of a fill or profit. Holding-time is a liquidity/freshness filter, not a prediction. The scanner ranks buy-side opportunities. If a selected item is in inventory, it can guide a manual sale, but it does not know your cost basis or realized P&L/ROI. Active-offer guidance cannot determine offer age or whether to cancel.

A RuneLite Plugin Hub listing is not Jagex approval. Review the current [Jagex third-party client guidelines](https://secure.runescape.com/m=news/third-party-client-guidelines?oldschool=1) and [RuneLite Plugin Hub requirements](https://github.com/runelite/plugin-hub#reviewing).

See [THIRD_PARTY_NOTICES](./THIRD_PARTY_NOTICES) for attribution and license terms for the suggested-search interaction pattern.

## Build and test

Requires JDK 11. Run `./gradlew test build` (Windows: `gradlew.bat test build`).

Market analysis checks run as part of Gradle's `check` task.
