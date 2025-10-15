package com.castlewarsindicators;

import com.castlewarsindicators.CastleWarsIndicatorsPlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class CastleWarsIndicatorsPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(CastleWarsIndicatorsPlugin.class);
		RuneLite.main(args);
	}
}