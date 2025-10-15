package com.castlewarsindicators;

import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;

import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

import java.awt.image.BufferedImage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Slf4j
@PluginDescriptor(
	name = "Castle Wars Indicators"
)
public class CastleWarsIndicatorsPlugin extends Plugin
{
	@Inject private Client client;
	@Inject private CastleWarsIndicatorsConfig config;
	@Inject private Notifier notifier;

	@Inject private InfoBoxManager infoBoxManager;
	@Inject private ItemManager itemManager;

	// === Groups ===
	private static final int GROUP_SARA = 58;
	private static final int GROUP_ZAM  = 59;
	private static final int GROUP_WAITING_ROOM = 131;

	// === Respawn warning widgets ===
	private static final int SARA_WIDGET = 3801114;  // (group 58)
	private static final int ZAM_WIDGET  = 3866649;  // (group 59)


	// === Score widgets ===
	private static final int SARA_SARADOMIN_SCORE = 3801116; // "1 = Saradomin"
	private static final int SARA_ZAMORAK_SCORE   = 3801117; // "Zamorak = 1"
	private static final int ZAM_ZAMORAK_SCORE    = 3866650; // "Zamorak = 0"
	private static final int ZAM_SARADOMIN_SCORE  = 3866651; // "1 = Saradomin"

	// === Game timer (minutes left) ===
	private static final int VARP_CASTLEWARS_TIMER = 380;

	// === State Flags ===
	private Integer activeGroupId = null;
	private boolean inWaitingRoom = false;
	private boolean spawnWarningActive = false;
	private long lastRepeatNotifyMs = 0L;

	// === Lobby State ===
	private boolean chatOptionsOpen = false;
	private boolean joinPopupVisible = false;

	// === InfoBox ===
	private CastleWarsImbalanceInfoBox imbalanceBox;
	private static final Pattern DIGIT_PATTERN = Pattern.compile("(\\d+)");

	// === Lobby Join Message ===
	private static final int GROUP_CHAT_OPTIONS = 219;   // Chatmenu.OPTIONS
	private static final int CHAT_OPTIONS_CHILD = 1;     // child
	private static final int JOIN_POPUP_PACKED  = (GROUP_CHAT_OPTIONS << 16) | CHAT_OPTIONS_CHILD; // 14352385
	private static final String JOIN_POPUP_TEXT = "There's a free space, do you want to join?";



	@Override
	protected void startUp()
	{
		reset();
		//log.info("Castle Wars Improved started!");
	}

	@Override
	protected void shutDown()
	{
		removeImbalanceBox();
		reset();
		//log.info("Castle Wars Improved stopped!");
	}

	private void reset()
	{
		activeGroupId = null;
		inWaitingRoom = false;
		joinPopupVisible = false;
		spawnWarningActive = false;
		lastRepeatNotifyMs = 0L;
	}

	// === Lobby / waiting room detection ===
	@Subscribe
	public void onWidgetLoaded(WidgetLoaded e)
	{
		final int g = e.getGroupId();

		if (g == GROUP_SARA || g == GROUP_ZAM)
		{
			activeGroupId = g;
			spawnWarningActive = false;
			lastRepeatNotifyMs = 0L;

			if (config.notifyLobbyStartStop())
			{
				String msg = "Castle War: Leave the room to go AFK";
				//client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", msg, null);
				notifier.notify(msg);
			}
			//log.info("Castle Wars group {} loaded", g);
		}
		if (g == GROUP_WAITING_ROOM)
		{
			inWaitingRoom = true;
			joinPopupVisible = false;
			//log.info("Castle Wars waiting room loaded (group 131)");
		}

		// Join message (219)
		if (g == GROUP_CHAT_OPTIONS)
		{
			chatOptionsOpen = true;
			//log.debug("Chat options opened (group 219)");
		}
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed e)
	{
		final int g = e.getGroupId();

		if (g == GROUP_SARA || g == GROUP_ZAM)
		{
			if (config.notifyLobbyStartStop())
			{
				String msg = "Castle War Minigame Completed!";
				//client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", msg, null);
				notifier.notify(msg);
			}
			activeGroupId = null;
			spawnWarningActive = false;
			lastRepeatNotifyMs = 0L;
			removeImbalanceBox();
			//log.info("Castle Wars group {} closed", g);
		}
		if (g == GROUP_WAITING_ROOM)
		{
			inWaitingRoom = false;
			joinPopupVisible = false;
			//log.info("Castle Wars waiting room closed (group 131)");
		}

		// Join message
		if (g == GROUP_CHAT_OPTIONS)
		{
			chatOptionsOpen = false;
			joinPopupVisible = false;
			//log.debug("Chat options closed (group 219)");
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		handleWaitingRoomJoinPopup();
		handleSpawnWarning();
		updateImbalanceInfoBox();
	}

	// --- Waiting room join popup ---
	private void handleWaitingRoomJoinPopup()
	{

		if (!config.notifyJoinPopup())
		{
			joinPopupVisible = false;
			return;
		}

		boolean visibleNow = false;

		// Waiting room id (219,1)
		Widget root = client.getWidget(JOIN_POPUP_PACKED);
		if (root != null && !root.isHidden())
		{
			String t = root.getText();
			if (t != null && t.contains(JOIN_POPUP_TEXT))
			{
				visibleNow = true;
			}
		}

		// Fallback: scan (219,1)'s subtree for text
		if (!visibleNow && chatOptionsOpen)
		{
			if (root == null)
			{
				root = client.getWidget(GROUP_CHAT_OPTIONS, CHAT_OPTIONS_CHILD);
			}
			if (root != null)
			{
				Widget hit = findVisibleTextWidget(root, JOIN_POPUP_TEXT);
				visibleNow = (hit != null);
			}
		}

		// Edge trigger - notify once on appearance
		if (visibleNow && !joinPopupVisible)
		{
			String msg = "Castle Wars: A free spot is available!";
			//client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", msg, null);
			notifier.notify(msg);
			//log.info("Join popup detected (219,1)");
		}

		joinPopupVisible = visibleNow;
	}

	// DFS search for text
	private Widget findVisibleTextWidget(Widget w, String needle)
	{
		if (!w.isHidden())
		{
			String t = w.getText();
			if (t != null && t.contains(needle))
			{
				return w;
			}
		}

		Widget[] kids = w.getChildren();
		if (kids != null)
		{
			for (Widget c : kids)
			{
				Widget hit = findVisibleTextWidget(c, needle);
				if (hit != null) return hit;
			}
		}

		Widget[] dyn = w.getDynamicChildren();
		if (dyn != null)
		{
			for (Widget c : dyn)
			{
				Widget hit = findVisibleTextWidget(c, needle);
				if (hit != null) return hit;
			}
		}

		Widget[] nested = w.getNestedChildren();
		if (nested != null)
		{
			for (Widget c : nested)
			{
				Widget hit = findVisibleTextWidget(c, needle);
				if (hit != null) return hit;
			}
		}

		return null;
	}

	// --- Respawn warning ---
	private void handleSpawnWarning()
	{
		if (activeGroupId == null)
		{
			spawnWarningActive = false;
			return;
		}

		final int widgetId = (activeGroupId == GROUP_ZAM) ? ZAM_WIDGET : SARA_WIDGET;
		final Widget w = client.getWidget(widgetId);

		if (w == null || w.getText() == null || w.getText().isEmpty())
		{
			spawnWarningActive = false;
			lastRepeatNotifyMs = 0L;
			return;
		}

		final String text = w.getText();
		final boolean inSpawnWarning =
				text.contains("<col=ffff00>You have 2 minutes to leave the respawn room.") ||
						text.contains("<col=ff0000>WARNING: YOU HAVE LESS THAN A MINUTE TO LEAVE THE RESPAWN ROOM!");

		if (inSpawnWarning && !spawnWarningActive && config.notifyOnSpawnWarning())
		{
			String msg = "Castle Wars: Leave the respawn room!";
			//client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", msg, null);
			notifier.notify(msg);
		}

		if (inSpawnWarning && config.enablePersistentNotify())
		{
			final long now = System.currentTimeMillis();
			final long intervalMs =
					Math.max(5, Math.min(120, config.persistentNotifyIntervalSeconds())) * 1000L;

			if (now - lastRepeatNotifyMs >= intervalMs)
			{
				String msg = "Castle Wars: Leave the respawn room!";
				//client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", msg, null);
				notifier.notify(msg);
				lastRepeatNotifyMs = now;
			}
		}
		else if (!inSpawnWarning)
		{
			lastRepeatNotifyMs = 0L;
		}

		spawnWarningActive = inSpawnWarning;
	}

	// --- Balanced / Imbalanced infobox ---
	private void updateImbalanceInfoBox()
	{
		if (!config.showImbalanceStatus() || activeGroupId == null)
		{
			removeImbalanceBox();
			return;
		}

		final int minutesLeft = client.getVarpValue(VARP_CASTLEWARS_TIMER);
		if (minutesLeft <= 0 || minutesLeft > 15)
		{
			// Only hide the box not in 15-minutes window
			removeImbalanceBox();
			return;
		}

		// Ensure the box exists
		ensureImbalanceBox();
		if (imbalanceBox == null)
		{
			return;
		}

		// Default
		String label = "Balanced";

		// widget IDs for Scores based on team
		final int saraScoreId = (activeGroupId == GROUP_SARA) ? SARA_SARADOMIN_SCORE : ZAM_SARADOMIN_SCORE;
		final int zamScoreId  = (activeGroupId == GROUP_SARA) ? SARA_ZAMORAK_SCORE   : ZAM_ZAMORAK_SCORE;

		Integer saraScore = readScoreFromWidget(saraScoreId);
		Integer zamScore  = readScoreFromWidget(zamScoreId);

		// If both scores are readable, decide balance; otherwise keep default "Balanced"
		if (saraScore != null && zamScore != null)
		{
			label = (!saraScore.equals(zamScore)) ? "Unbalanced" : "Balanced";
		}

		imbalanceBox.setLabel(label);
	}

	// Score Format Zamorack = ($) ; ($) = Saradomin
	private Integer readScoreFromWidget(int packedId)
	{
		Widget w = client.getWidget(packedId);
		if (w == null || w.isHidden())
		{
			return null;
		}
		String t = w.getText();
		if (t == null || t.isEmpty())
		{
			return null;
		}

		Matcher m = DIGIT_PATTERN.matcher(t);
		if (!m.find())
		{
			return null;
		}
		try
		{
			return Integer.parseInt(m.group(1));
		}
		catch (NumberFormatException ex)
		{
			return null;
		}
	}

	private void ensureImbalanceBox()
	{
		if (imbalanceBox != null)
		{
			return;
		}

		// 4067 = Castle Wars flag
		BufferedImage img = itemManager.getImage(4067);
		if (img == null)
		{
			return;
		}

		imbalanceBox = new CastleWarsImbalanceInfoBox(img, this);
		infoBoxManager.addInfoBox(imbalanceBox);
	}

	private void removeImbalanceBox()
	{
		if (imbalanceBox != null)
		{
			infoBoxManager.removeInfoBox(imbalanceBox);
			imbalanceBox = null;
		}
	}

	@Provides
	CastleWarsIndicatorsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CastleWarsIndicatorsConfig.class);
	}
}
