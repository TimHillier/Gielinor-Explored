package com.gielinorexplored;

import lombok.Value;

@Value
public class ExploredTile {
  private int regionId;
  private int regionX;
  private int regionY;
  private int plane;
}
