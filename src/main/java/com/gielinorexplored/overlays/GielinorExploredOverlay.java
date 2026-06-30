package com.gielinorexplored.overlays;

import com.gielinorexplored.GielinorExploredConfig;
import com.gielinorexplored.GielinorExploredPlugin;
import com.gielinorexplored.utils.GielinorExploredTileUtils;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.util.Collection;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Tile;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Overlay that renders the fog of war effect on the game's map.
 */
public class GielinorExploredOverlay extends Overlay {
  private static final int MAX_DRAW_DISTANCE = 32;
  private final Client client;
  private final GielinorExploredPlugin plugin;
  private final GielinorExploredConfig config;
  private final GielinorExploredTileUtils tileUtils;

  @Inject
  private GielinorExploredOverlay(
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
    setLayer(OverlayLayer.ABOVE_SCENE);
  }

  @Override
  public Dimension render(Graphics2D graphics) {
    if (!config.enableFog()) {
      return null;
    }

    if (client.getLocalPlayer() == null) {
      return null;
    }

    if (plugin.lastPlane != client.getTopLevelWorldView().getPlane()) {
      tileUtils.updateExploredTiles();
      plugin.updateLastPlane();
    }

    int viewportX = client.getViewportXOffset();
    int viewportY = client.getViewportYOffset();
    int viewportW = client.getViewportWidth();
    int viewportH = client.getViewportHeight();

    final Shape prevClip = graphics.getClip();
    final Composite prevComposite = graphics.getComposite();

    graphics.clipRect(viewportX, viewportY, viewportW, viewportH);

    /* Apply Fog */
    graphics.setComposite(AlphaComposite.SrcOver);
    graphics.setColor(getFogColor());
    graphics.fillRect(viewportX, viewportY, viewportW, viewportH);

    /* Clear Fog from Visited Points. */
    graphics.setComposite(AlphaComposite.Clear);

    final Collection<WorldPoint> visitedPoints = tileUtils.getExploredTiles();
    WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();
    Tile[][][] tiles = client.getTopLevelWorldView().getScene().getTiles();
    int plane = plugin.lastPlane;

    for (int x = 0; x < tiles[plane].length; x++) {
      for (int y = 0; y < tiles[plane][x].length; y++) {
        Tile tile = tiles[plane][x][y];
        if (tile == null) {
          continue;
        }

        WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, tile.getLocalLocation());
        if (worldPoint.getPlane() != plane) {
          continue;
        }

        if (!visitedPoints.contains(worldPoint)) {
          continue;
        }

        if (worldPoint.distanceTo(playerLocation) >= MAX_DRAW_DISTANCE) {
          continue;
        }

        Polygon polygon = Perspective.getCanvasTilePoly(client, tile.getLocalLocation());
        if (polygon != null) {
          graphics.fill(polygon);
        }
      }
    }

    unHidePlayer(graphics, client.getLocalPlayer());
    graphics.setComposite(prevComposite);
    graphics.setClip(prevClip);
    return null;
  }

  private void unHidePlayer(Graphics2D graphics, Player player) {
    if (player == null) {
      return;
    }

    Shape playerShape = player.getConvexHull();
    if (playerShape != null) {
      graphics.fill(playerShape);
      return;
    }

    Polygon tilePolygon = player.getCanvasTilePoly();
    if (tilePolygon != null) {
      graphics.fill(tilePolygon);
    }
  }

  private Color getFogColor() {
    return config.fogColor();
  }
}
