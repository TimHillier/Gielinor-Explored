package com.gielinorexplored;

import lombok.Value;

/**
 * Represents an explored tile on the game's map and world map.
 */
@Value
public class ExploredTile {
  private int regionId;
  private int regionX;
  private int regionY;
  private int plane;
}
