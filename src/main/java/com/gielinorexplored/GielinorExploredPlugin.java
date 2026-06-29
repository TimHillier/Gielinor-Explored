package com.gielinorexplored;

import com.gielinorexplored.overlays.*;
import com.gielinorexplored.utils.*;
import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(name = "Gielinor Explored")
public class GielinorExploredPlugin extends Plugin {
  public int lastPlane;
  @Inject private GielinorExploredTileUtils tileUtils;
  @Inject private GielinorExploredMovementUtils movementUtils;
  @Inject private GielinorExploredOverlay gielinorExploredOverlay;
  @Inject private GielinorExploredMiniMapOverlay gielinorExploredMiniMapOverlay;
  @Inject private GielinorExploredWorldMapOverlay gielinorExploredWorldMapOverlay;
  @Inject private Client client;
  @Inject private OverlayManager overlayManager;

  @Override
  protected void startUp() throws Exception {
    log.debug("Gielinor Explored Starting up.");
    overlayManager.add(gielinorExploredOverlay);
    overlayManager.add(gielinorExploredMiniMapOverlay);
    overlayManager.add(gielinorExploredWorldMapOverlay);
    updateLastPlane();
    tileUtils.updateExploredTiles();
  }

  @Override
  protected void shutDown() throws Exception {
    overlayManager.remove(gielinorExploredOverlay);
    overlayManager.remove(gielinorExploredMiniMapOverlay);
    overlayManager.remove(gielinorExploredWorldMapOverlay);
    tileUtils.clearExploredTiles();
    log.debug("Gielinor Explored Shutting Down.");
  }

  @Provides
  GielinorExploredConfig provideConfig(ConfigManager configManager) {
    return configManager.getConfig(GielinorExploredConfig.class);
  }

  @Subscribe
  public void onGameTick(GameTick tick) {
    movementUtils.addCurrentTile();
  }

  @Subscribe
  public void onGameStateChanged(GameStateChanged gameStateChanged) {
    if (gameStateChanged.getGameState() != GameState.LOGGED_IN) {
      movementUtils.setLastTile(null);
    }

    tileUtils.updateExploredTiles();
    updateLastPlane();
  }

  public void updateLastPlane() {
    lastPlane = client.getTopLevelWorldView().getPlane();
    movementUtils.setLastPlane(lastPlane);
  }
}
