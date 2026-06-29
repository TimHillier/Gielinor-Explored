/*
 * Copyright (c) 2018, TheLonelyDev <https://github.com/TheLonelyDev>
 * Copyright (c) 2018, Adam <Adam@sigterm.info>
 * Copyright (c) 2020, ConorLeckey <https://github.com/ConorLeckey>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

// Idk how necessary this is, but I adapted their
// movement and tile management code for this project, and I want to make sure
// They get the credit they deserve for it.
package com.gielinorexplored.utils;

import com.gielinorexplored.ExploredTile;
import java.util.*;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.config.ConfigManager;

@Singleton
public class GielinorExploredTileUtils {
  private static final String CONFIG_GROUP = "gielinorExplored";
  private static final String DATA_PREFIX = "region_data";
  @Getter private final List<WorldPoint> exploredTiles = new ArrayList<>();
  private final Client client;
  private final ConfigManager configManager;

  @Inject
  GielinorExploredTileUtils(Client client, ConfigManager configManager) {
    this.client = client;
    this.configManager = configManager;
  }

  private Collection<WorldPoint> translateToWorldPoint(Collection<ExploredTile> points) {
    if (points.isEmpty()) {
      return Collections.EMPTY_LIST;
    }

    return points.stream()
        .map(
            point ->
                WorldPoint.fromRegion(
                    point.getRegionId(), point.getRegionX(), point.getRegionY(), point.getPlane()))
        .flatMap(
            worldPoint -> {
              final Collection<WorldPoint> localWorldPoints =
                  WorldPoint.toLocalInstance(client, worldPoint);
              return localWorldPoints.stream();
            })
        .collect(Collectors.toList());
  }

  private Collection<ExploredTile> decodeTiles(int regionId, int plane) {
    List<ExploredTile> exploredTileList = new ArrayList<>();
    String key = getKey(regionId, plane);
    String encoded = configManager.getConfiguration(CONFIG_GROUP, key);

    if (encoded == null) {
      return exploredTileList;
    }

    byte[] bytes = Base64.getUrlDecoder().decode(encoded);
    if (bytes == null || bytes.length == 0) {
      return exploredTileList;
    }

    BitSet bitSet = BitSet.valueOf(bytes);

    for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i + 1)) {
      int tileRegionY = i / 64;
      int tileRegionX = i - (64 * tileRegionY);
      exploredTileList.add(new ExploredTile(regionId, tileRegionX, tileRegionY, plane));
    }

    return exploredTileList;
  }

  private String getKey(int regionId, int plane) {
    return DATA_PREFIX + "_" + regionId + "_" + plane;
  }

  public Collection<ExploredTile> getTiles(int regionId, int plane) {
    return decodeTiles(regionId, plane);
  }

  public void clearExploredTiles() {
    exploredTiles.clear();
  }

  public void updateExploredTiles() {
    exploredTiles.clear();

    int[] regions = client.getTopLevelWorldView().getMapRegions();
    if (regions == null) {
      return;
    }

    for (int regionId : regions) {
      Collection<WorldPoint> worldPoint =
          translateToWorldPoint(getTiles(regionId, client.getTopLevelWorldView().getPlane()));
      exploredTiles.addAll(worldPoint);
    }
  }

  public void updateTileMark(LocalPoint localPoint) {
    int plane = client.getTopLevelWorldView().getPlane();
    WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, localPoint);
    int regionId = worldPoint.getRegionID();
    ExploredTile exploredTile =
        new ExploredTile(regionId, worldPoint.getRegionX(), worldPoint.getRegionY(), plane);
    Collection<ExploredTile> regionTiles = getTiles(regionId, plane);

    if (!regionTiles.contains(exploredTile)) {
      regionTiles.add(exploredTile);
      exploredTiles.add(worldPoint);
      addTiles(regionTiles, regionId, plane);
    }
  }

  private void addTiles(Collection<ExploredTile> tiles, int regionId, int plane) {
    String key = getKey(regionId, plane);
    encodeTiles(tiles, key);
  }

  private void encodeTiles(Collection<ExploredTile> tiles, String key) {
    if (tiles == null || tiles.isEmpty()) {
      configManager.unsetConfiguration(CONFIG_GROUP, key);
      return;
    }

    BitSet output = new BitSet(4096);
    for (ExploredTile tile : tiles) {
      output.set(tile.getRegionY() * 64 + tile.getRegionX());
    }

    configManager.setConfiguration(CONFIG_GROUP, key, output.toByteArray());
  }
}
