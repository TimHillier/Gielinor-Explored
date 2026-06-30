package com.gielinorexplored.overlays;

import com.gielinorexplored.ExploredTile;
import com.gielinorexplored.GielinorExploredConfig;
import com.gielinorexplored.utils.GielinorExploredTileUtils;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class GielinorExploredWorldMapOverlay extends Overlay {
  private static final int REGION_SIZE = 1 << 6;
  private static final int REGION_TRUNCATE = ~((1 << 6) - 1);
  private final Client client;
  private final GielinorExploredConfig config;
  private final GielinorExploredTileUtils tileUtils;
  private int fogImageHeight = -1;
  private int fogImageWidth = -1;
  private BufferedImage fogBufferedImage;
  private Graphics2D fogGraphics;

  @Inject
  private GielinorExploredWorldMapOverlay(
      Client client, GielinorExploredConfig config, GielinorExploredTileUtils tileUtils) {
    this.client = client;
    this.config = config;
    this.tileUtils = tileUtils;
    setPosition(OverlayPosition.DYNAMIC);
    setPriority(Overlay.PRIORITY_HIGH);
    setLayer(OverlayLayer.ABOVE_WIDGETS);
  }

  @Override
  public Dimension render(Graphics2D graphics) {
    if (!config.showOnWorldMap()) {
      return null;
    }

    drawOnWorldMap(graphics);
    return null;
  }

  private void drawOnWorldMap(Graphics2D graphics) {
    Widget worldMap = client.getWidget(ComponentID.WORLD_MAP_MAPVIEW);
    if (worldMap == null) {
      return;
    }

    Float pixelsPerTile = client.getWorldMap().getWorldMapZoom();
    Rectangle worldMapRectangle = worldMap.getBounds();
    Graphics2D fogGraphic = getFogGraphic(worldMapRectangle);

    // Apply Fog
    fogGraphic.setComposite(AlphaComposite.Src);
    fogGraphic.setColor(getFogColor());
    fogGraphic.fillRect(0, 0, worldMapRectangle.width, worldMapRectangle.height);

    int tileWidth = (int) Math.ceil(worldMapRectangle.getWidth() / pixelsPerTile);
    int tileHeight = (int) Math.ceil(worldMapRectangle.getHeight() / pixelsPerTile);
    Point worldMapPosition = client.getWorldMap().getWorldMapPosition();

    int yTileMin = worldMapPosition.getY() - tileHeight / 2;
    int xRegionMin = (worldMapPosition.getX() - tileWidth / 2) & REGION_TRUNCATE;
    int xRegionMax = ((worldMapPosition.getX() + tileWidth / 2) & REGION_TRUNCATE) + REGION_SIZE;
    int yRegionMin = (yTileMin & REGION_TRUNCATE);
    int yRegionMax = ((worldMapPosition.getY() + tileHeight / 2) & REGION_TRUNCATE) + REGION_SIZE;
    int regionPixelSize = (int) Math.ceil(REGION_SIZE * pixelsPerTile);

    // Clear Fog.
    fogGraphic.setComposite(AlphaComposite.Clear);

    for (int x = xRegionMin; x < xRegionMax; x += REGION_SIZE) {
      for (int y = yRegionMin; y < yRegionMax; y += REGION_SIZE) {
        int regionId = ((x >> 6) << 8) | (y >> 6);
        int plane = client.getTopLevelWorldView().getPlane();

        for (final ExploredTile tile : tileUtils.getTiles(regionId, plane)) {
          if (tile.getPlane() != plane) {
            continue;
          }
          int yTileOffset = -(yTileMin - y) + 2;
          int xTileOffset = x + tileWidth / 2 - worldMapPosition.getX();

          int xPos = ((int) (xTileOffset * pixelsPerTile));
          int yPos = (worldMapRectangle.height - (int) (yTileOffset * pixelsPerTile));

          int size = (regionPixelSize / (64 - Math.round(48f * ((8f - pixelsPerTile) / 7f))));
          int tileSize = regionPixelSize / 64;

          fogGraphic.fillRect(
              xPos + (tile.getRegionX() * tileSize),
              yPos - (tile.getRegionY() * tileSize) + tileSize,
              size - 1,
              size - 1);
        }
      }
    }
    graphics.drawImage(fogBufferedImage, worldMapRectangle.x, worldMapRectangle.y, null);
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

  private Color getFogColor() {
    return config.fogColor();
  }
}
