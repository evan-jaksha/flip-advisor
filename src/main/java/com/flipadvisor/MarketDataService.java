package com.flipadvisor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

final class MarketDataService
{
	private static final String API_ROOT = "https://prices.runescape.wiki/api/v2/osrs";
	private static final String USER_AGENT = "FlipAdvisor/1.0 (RuneLite local market research plugin)";
	private final OkHttpClient httpClient;

	MarketDataService(OkHttpClient httpClient)
	{
		this.httpClient = httpClient;
	}

	List<MarketItem> loadMarketData() throws IOException
	{
		JsonElement mapping = getJson(API_ROOT + "/mapping");
		JsonObject latestPrices = getJson(API_ROOT + "/latest").getAsJsonObject().getAsJsonObject("data");
		JsonObject hourlyPrices = getJson(API_ROOT + "/1h").getAsJsonObject().getAsJsonObject("data");
		if (latestPrices == null || hourlyPrices == null)
		{
			throw new IOException("The prices API returned an incomplete response");
		}

		Map<Integer, ItemMapping> mappings = new HashMap<>();
		for (JsonElement element : mapping.getAsJsonArray())
		{
			JsonObject object = element.getAsJsonObject();
			int id = readInt(object, "id", -1);
			String name = readString(object, "name");
			int limit = readInt(object, "limit", 0);
			if (id > 0 && name != null && limit > 0)
			{
				mappings.put(id, new ItemMapping(name, limit));
			}
		}

		List<MarketItem> items = new ArrayList<>();
		for (Map.Entry<String, JsonElement> entry : latestPrices.entrySet())
		{
			int id;
			try
			{
				id = Integer.parseInt(entry.getKey());
			}
			catch (NumberFormatException ex)
			{
				continue;
			}

			ItemMapping itemMapping = mappings.get(id);
			JsonElement hourlyElement = hourlyPrices.get(String.valueOf(id));
			if (itemMapping == null || hourlyElement == null || hourlyElement.isJsonNull())
			{
				continue;
			}

			JsonObject latest = entry.getValue().getAsJsonObject();
			JsonObject hourly = hourlyElement.getAsJsonObject();
			long high = readLong(latest, "high", 0);
			long low = readLong(latest, "low", 0);
			long highTime = readLong(latest, "highTime", 0);
			long lowTime = readLong(latest, "lowTime", 0);
			long volume = readLong(hourly, "highPriceVolume", 0)
				+ readLong(hourly, "lowPriceVolume", 0);

			items.add(new MarketItem(
				id,
				itemMapping.name,
				itemMapping.limit,
				high,
				low,
				highTime,
				lowTime,
				volume,
				readDouble(hourly, "avgHighPrice", 0),
				readDouble(hourly, "avgLowPrice", 0)));
		}
		return items;
	}

	private JsonElement getJson(String url) throws IOException
	{
		Request request = new Request.Builder()
			.url(url)
			.header("User-Agent", USER_AGENT)
			.build();
		try (Response response = httpClient.newCall(request).execute())
		{
			if (!response.isSuccessful())
			{
				throw new IOException("Prices API returned HTTP " + response.code());
			}
			ResponseBody body = response.body();
			if (body == null)
			{
				throw new IOException("Prices API returned an empty response");
			}
			return new JsonParser().parse(body.string());
		}
	}

	private static long readLong(JsonObject object, String key, long defaultValue)
	{
		JsonElement value = object.get(key);
		return value == null || value.isJsonNull() ? defaultValue : value.getAsLong();
	}

	private static int readInt(JsonObject object, String key, int defaultValue)
	{
		JsonElement value = object.get(key);
		return value == null || value.isJsonNull() ? defaultValue : value.getAsInt();
	}

	private static double readDouble(JsonObject object, String key, double defaultValue)
	{
		JsonElement value = object.get(key);
		return value == null || value.isJsonNull() ? defaultValue : value.getAsDouble();
	}

	private static String readString(JsonObject object, String key)
	{
		JsonElement value = object.get(key);
		return value == null || value.isJsonNull() ? null : value.getAsString();
	}

	private static final class ItemMapping
	{
		private final String name;
		private final int limit;

		private ItemMapping(String name, int limit)
		{
			this.name = name;
			this.limit = limit;
		}
	}
}
