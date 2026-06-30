package com.gielinorexplored.overlays;

import static net.runelite.api.widgets.ComponentID.MINIMAP_CONTAINER;

import com.gielinorexplored.GielinorExploredConfig;
import com.gielinorexplored.GielinorExploredPlugin;
import com.gielinorexplored.utils.GielinorExploredTileUtils;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Overlay that renders the fog of war effect on the game's minimap.
 */
public class GielinorExploredMiniMapOverlay extends Overlay {
  private static final int MAX_DRAW_DISTANCE = 16;
  private static final int TILE_WIDTH = 4;
  private static final int TILE_HEIGHT = 4;

  private final Client client;
  private final GielinorExploredPlugin plugin;
  private final GielinorExploredTileUtils tileUtils;
  private final GielinorExploredConfig config;
  private int fogImageWidth = -1;
  private int fogImageHeight = -1;
  private BufferedImage fogBufferedImage;
  private Graphics2D fogGraphics;

  @Inject
  private GielinorExploredMiniMapOverlay(
      Client client,
      GielinorExploredConfig config,
      GielinorExploredPlugin plugin,
      GielinorExploredTileUtils tileUtils) {
    this.client = client;
    this.plugin = plugin;
    this.config = config;
    this.tileUtils = tileUtils;
    setPosition(OverlayPosition.DYNAMIC);
    setPriority(Overlay.PRIORITY_LOW);
    setLayer(OverlayLayer.ABOVE_WIDGETS);
  }

  @Override
  public Dimension render(Graphics2D graphics) {
    if (!config.showOnMiniMap()) {
      return null;
    }

    if (client.getLocalPlayer() == null) {
      return null;
    }

    if (plugin.lastPlane != client.getTopLevelWorldView().getPlane()) {
      tileUtils.updateExploredTiles();
      plugin.updateLastPlane();
    }

    Widget minimap = client.getWidget(MINIMAP_CONTAINER);
    if (minimap == null || minimap.isHidden()) {
      return null;
    }

    Rectangle bounds = minimap.getBounds();
    Graphics2D fogGraphic = getFogGraphic(bounds);

    // Apply Fog
    fogGraphic.setComposite(AlphaComposite.Src);
    fogGraphic.setColor(getFogColor());
    fogGraphic.fillRect(0, 0, bounds.width, bounds.height);

    // Remove walked areas.
    fogGraphic.setComposite(AlphaComposite.Clear);
    WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();
    for (WorldPoint worldPoint : tileUtils.getExploredTiles()) {
      if (worldPoint.getPlane() != playerLocation.getPlane()) {
        continue;
      }
      if (worldPoint.distanceTo(playerLocation) >= MAX_DRAW_DISTANCE) {
        continue;
      }
      LocalPoint localPoint = LocalPoint.fromWorld(client, worldPoint);
      if (localPoint == null) {
        continue;
      }
      Point posOnMinimap = Perspective.localToMinimap(client, localPoint);
      if (posOnMinimap == null) {
        continue;
      }
      fogGraphic.fillRect(
          posOnMinimap.getX() - bounds.x - TILE_WIDTH / 2,
          posOnMinimap.getY() - bounds.y - TILE_HEIGHT / 2,
          TILE_WIDTH,
          TILE_HEIGHT);
    }

    final Shape prevClip = graphics.getClip();
    graphics.setClip(minimapCircle(bounds));
    graphics.drawImage(fogBufferedImage, bounds.x, bounds.y, null);
    graphics.setClip(prevClip);
    return null;
  }

  private Graphics2D getFogGraphic(Rectangle bounds) {
    int w = bounds.width;
    int h = bounds.height;
    if (fogBufferedImage == null || fogImageWidth != w || fogImageHeight != h) {
      if (fogGraphics != null) {
        fogGraphics.dispose();
      }
      fogBufferedImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
      fogGraphics = fogBufferedImage.createGraphics();
      fogImageWidth = w;
      fogImageHeight = h;
    }
    return fogGraphics;
  }

  private Ellipse2D minimapCircle(Rectangle bounds) {
    return new Ellipse2D.Double(
        bounds.x + 50, bounds.y + -2, bounds.width * .76, bounds.height * .79);
  }

  private Color getFogColor() {
    return config.fogColor();
  }
}
