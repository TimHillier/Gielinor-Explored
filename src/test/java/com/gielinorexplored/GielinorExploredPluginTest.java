package com.gielinorexplored;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class GielinorExploredPluginTest {
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(GielinorExploredPlugin.class);
		RuneLite.main(args);
	}
}