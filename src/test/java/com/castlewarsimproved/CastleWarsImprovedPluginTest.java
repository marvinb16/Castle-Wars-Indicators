package com.castlewarsimproved;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class CastleWarsImprovedPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(CastleWarsImprovedPlugin.class);
		RuneLite.main(args);
	}
}