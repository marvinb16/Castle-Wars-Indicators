package com.castlewarsimproved;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;
import net.runelite.client.config.Notification;


@ConfigGroup("castlewarsimproved")
public interface CastleWarsImprovedConfig extends Config
{
	@ConfigItem(
			position = 0,
			keyName = "notifications",
			name = "Notifications Config",
			description = "Configures all notifications"

	)
	default Notification notification() {
		return Notification.ON;
	}

	@ConfigItem(
			position = 1,
			keyName = "notifyLobbyStartStop",
			name = "Notify on game start/stop",
			description = "Notification when Castle Wars game starts/ends"
	)
	default boolean notifyLobbyStartStop() { return true; }

	@ConfigItem(
			position = 2,
			keyName = "notifyJoinPopup",
			name = "Notify on join popup",
			description = "Notification when a free slot is offered"
	)
	default boolean notifyJoinPopup() { return true; }

	@ConfigItem(
			position = 3,
			keyName = "notifyAtTwoMinutes",
			name = "Notify in Spawn Room",
			description = "One-time notify when in spawn area"
	)
	default boolean notifyOnSpawnWarning() { return true; }

	@ConfigItem(
			position = 4,
			keyName = "enablePersistentNotify",
			name = "Repeat while in Spawn Room",
			description = "Repeat notifications while in the spawn room"
	)
	default boolean enablePersistentNotify() { return false; }

	@ConfigItem(
			position = 5,
			keyName = "persistentNotifyIntervalSeconds",
			name = "Repeat interval",
			description = "Seconds between repeated notifications"
	)
	default int persistentNotifyIntervalSeconds() { return 20; }

	@ConfigItem(
			position = 6,
			keyName = "showImbalanceStatus",
			name = "Show team balance infobox",
			description = "Displays a 'Balanced/unbalanced' infobox when ≤ 15 minutes remain"
	)
	default boolean showImbalanceStatus() { return true; }


}