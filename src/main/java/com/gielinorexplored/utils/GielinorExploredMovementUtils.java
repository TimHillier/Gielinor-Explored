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

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.CollisionData;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

/**
 * Utility class for managing the player's movement and tile exploration.
 */
@Singleton
public class GielinorExploredMovementUtils {
  private final Client client;
  private final GielinorExploredTileUtils tileUtils;
  private final MovementFlag[] fullBlock =
      new MovementFlag[] {
        MovementFlag.BLOCK_MOVEMENT_FLOOR,
        MovementFlag.BLOCK_MOVEMENT_FLOOR_DECORATION,
        MovementFlag.BLOCK_MOVEMENT_OBJECT,
        MovementFlag.BLOCK_MOVEMENT_FULL
      };
  private final MovementFlag[] allDirections =
      new MovementFlag[] {
        MovementFlag.BLOCK_MOVEMENT_NORTH_WEST,
        MovementFlag.BLOCK_MOVEMENT_NORTH,
        MovementFlag.BLOCK_MOVEMENT_NORTH_EAST,
        MovementFlag.BLOCK_MOVEMENT_EAST,
        MovementFlag.BLOCK_MOVEMENT_SOUTH_EAST,
        MovementFlag.BLOCK_MOVEMENT_SOUTH,
        MovementFlag.BLOCK_MOVEMENT_SOUTH_WEST,
        MovementFlag.BLOCK_MOVEMENT_WEST
      };
  @Getter @Setter private LocalPoint lastTile;
  @Getter @Setter private int lastPlane;

  @Inject
  GielinorExploredMovementUtils(Client client, GielinorExploredTileUtils tileUtils) {
    this.client = client;
    this.tileUtils = tileUtils;
  }

  /**
   * Adds the player's current tile to the explored tile list when they move.
   */
  public void addCurrentTile() {
    final WorldPoint playerPos = client.getLocalPlayer().getWorldLocation();
    if (playerPos == null) {
      return;
    }
    final LocalPoint playerPosLocal = LocalPoint.fromWorld(client, playerPos);
    if (playerPosLocal == null) {
      return;
    }

    if ((lastTile == null
        || (lastTile.distanceTo(playerPosLocal) != 0 && lastPlane == playerPos.getPlane())
        || lastPlane != playerPos.getPlane())) {
      handleWalkedToTile(playerPosLocal);
      lastTile = playerPosLocal;
    }
  }

  /**
   * Handles tile exploration when the player walks to a new position.
   *
   * @param currentPlayerPosition the player's current local position
   */
  private void handleWalkedToTile(LocalPoint currentPlayerPosition) {
    if (currentPlayerPosition == null) {
      return;
    }
    tileUtils.updateTileMark(currentPlayerPosition);

    if (lastTile != null) {
      int diffX = currentPlayerPosition.getX() - lastTile.getX();
      int diffY = currentPlayerPosition.getY() - lastTile.getY();
      int halfDiffX = diffX / 2;
      int halfDiffY = diffY / 2;

      switch (lastTile.distanceTo(currentPlayerPosition)) {
        case 181:
          handleCornerMovement(diffX, diffY);
          break;
        case 256:
        case 362:
          exploreTile(new LocalPoint(lastTile.getX() + halfDiffX, lastTile.getY() + halfDiffY));
          break;
        case 286:
          handleKnightMovement(diffX, diffY);
          break;
        case 0:
        case 128:
        default:
          break;
      }
    }
  }

  /**
   * Adds skipped tiles to the explored list when the player moves in an L-shaped path.
   *
   * @param diffX x distance in local coordinates between the last tile and current position
   * @param diffY y distance in local coordinates between the last tile and current position
   */
  private void handleKnightMovement(int diffX, int diffY) {
    int halfDiffX = diffX / 2;
    int halfDiffY = diffY / 2;
    int tileBesideDiffX;
    int tileBesideDiffY;

    if (Math.abs(diffY) == 128) {
      tileBesideDiffX = diffX;
      tileBesideDiffY = 0;
    } else {
      tileBesideDiffX = 0;
      tileBesideDiffY = diffY;
    }

    MovementFlag[] tileBesidesFlagsArray =
        getTileMovementFlags(lastTile.getX() + tileBesideDiffX, lastTile.getY() + tileBesideDiffY);

    if (tileBesidesFlagsArray.length == 0) {
      exploreTile(
          new LocalPoint(
              lastTile.getX() + tileBesideDiffX / 2, lastTile.getY() + tileBesideDiffY / 2));
    } else if (containsAnyOf(fullBlock, tileBesidesFlagsArray)) {
      if (Math.abs(halfDiffY) == 64) {
        halfDiffY *= 2;
      } else if (Math.abs(halfDiffX) == 64) {
        halfDiffX *= 2;
      }
      exploreTile(new LocalPoint(lastTile.getX() + halfDiffX, lastTile.getY() + halfDiffY));
    } else if (containsAnyOf(allDirections, tileBesidesFlagsArray)) {
      MovementFlag direction1;
      MovementFlag direction2;
      if (diffY == 256 || diffY == -128) {
        direction1 = MovementFlag.BLOCK_MOVEMENT_SOUTH;
      } else {
        direction1 = MovementFlag.BLOCK_MOVEMENT_NORTH;
      }
      if (diffX == 256 || diffX == -128) {
        direction2 = MovementFlag.BLOCK_MOVEMENT_WEST;
      } else {
        direction2 = MovementFlag.BLOCK_MOVEMENT_EAST;
      }

      if (containsAnyOf(tileBesidesFlagsArray, new MovementFlag[] {direction1, direction2})) {
        if (halfDiffY == 64) {
          halfDiffY = 128;
        } else if (halfDiffX == 64) {
          halfDiffX = 128;
        }
        exploreTile(new LocalPoint(lastTile.getX() + halfDiffX, lastTile.getY() + halfDiffY));
      } else {
        exploreTile(
            new LocalPoint(
                lastTile.getX() + tileBesideDiffX / 2, lastTile.getY() + tileBesideDiffY / 2));
      }
    }
  }

  /**
   * Adds skipped tiles to the explore list when the player moves diagonally and skips intermediate
   * tiles.
   *
   * @param diffX x distance in local coordinates between the last tile and current position
   * @param diffY y distance in local coordinates between the last tile and current position
   */
  private void handleCornerMovement(int diffX, int diffY) {
    LocalPoint northPoint;
    LocalPoint southPoint;

    if (diffY > 0) {
      northPoint = new LocalPoint(lastTile.getX(), lastTile.getY() + diffY);
      southPoint = new LocalPoint(lastTile.getX() + diffX, lastTile.getY());
    } else {
      northPoint = new LocalPoint(lastTile.getX() + diffX, lastTile.getY());
      southPoint = new LocalPoint(lastTile.getX(), lastTile.getY() + diffY);
    }

    MovementFlag[] northTile = getTileMovementFlags(northPoint);
    MovementFlag[] southTile = getTileMovementFlags(southPoint);

    if (diffX + diffY == 0) {
      if (containsAnyOf(fullBlock, northTile)
          || containsAnyOf(
              northTile,
              new MovementFlag[] {
                MovementFlag.BLOCK_MOVEMENT_SOUTH, MovementFlag.BLOCK_MOVEMENT_WEST
              })) {
        exploreTile(southPoint);
      } else if (containsAnyOf(fullBlock, southTile)
          || containsAnyOf(
              southTile,
              new MovementFlag[] {
                MovementFlag.BLOCK_MOVEMENT_NORTH, MovementFlag.BLOCK_MOVEMENT_EAST
              })) {
        exploreTile(northPoint);
      }
    } else {
      if (containsAnyOf(fullBlock, northTile)
          || containsAnyOf(
              northTile,
              new MovementFlag[] {
                MovementFlag.BLOCK_MOVEMENT_SOUTH, MovementFlag.BLOCK_MOVEMENT_EAST
              })) {
        exploreTile(southPoint);
      } else if (containsAnyOf(fullBlock, southTile)
          || containsAnyOf(
              southTile,
              new MovementFlag[] {
                MovementFlag.BLOCK_MOVEMENT_NORTH, MovementFlag.BLOCK_MOVEMENT_WEST
              })) {
        exploreTile(northPoint);
      }
    }
  }

  /**
   * Returns movement collision flags for the tile at the given local coordinates.
   *
   * @param x local x coordinate
   * @param y local y coordinate
   * @return movement flags for the tile
   */
  private MovementFlag[] getTileMovementFlags(int x, int y) {
    LocalPoint pointBeside = new LocalPoint(x, y);

    CollisionData[] collisionData = client.getCollisionMaps();
    assert collisionData != null;
    int[][] collisionDataFlags = collisionData[client.getPlane()].getFlags();

    Set<MovementFlag> tilesBesideFlagsSet =
        MovementFlag.getSetFlags(
            collisionDataFlags[pointBeside.getSceneX()][pointBeside.getSceneY()]);
    MovementFlag[] tileBesideFlagsArray = new MovementFlag[tilesBesideFlagsSet.size()];
    tilesBesideFlagsSet.toArray(tileBesideFlagsArray);

    return tileBesideFlagsArray;
  }

  /**
   * Returns movement collision flags for the given local point.
   *
   * @param localPoint the tile position in local coordinates
   * @return movement flags for the tile
   */
  private MovementFlag[] getTileMovementFlags(LocalPoint localPoint) {
    return getTileMovementFlags(localPoint.getX(), localPoint.getY());
  }

  /**
   * Returns whether any of the given flags appear in the comparison flag set.
   *
   * @param comparisonFlags flags to search within
   * @param flagsToCompare flags to look for
   * @return true if at least one flag from {@code flagsToCompare} is in {@code comparisonFlags}
   */
  private boolean containsAnyOf(MovementFlag[] comparisonFlags, MovementFlag[] flagsToCompare) {
    if (comparisonFlags.length == 0) {
      return false;
    }
    for (MovementFlag flag : flagsToCompare) {
      if (Arrays.asList(comparisonFlags).contains(flag)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Marks a tile as explored on the current plane if the player hasn't changed planes.
   *
   * @param localPoint the tile to mark
   */
  private void exploreTile(LocalPoint localPoint) {
    if (lastPlane != client.getPlane()) {
      return;
    }
    tileUtils.updateTileMark(localPoint);
  }

  @AllArgsConstructor
  enum MovementFlag {
    BLOCK_MOVEMENT_NORTH_WEST(CollisionDataFlag.BLOCK_MOVEMENT_NORTH_WEST),
    BLOCK_MOVEMENT_NORTH(CollisionDataFlag.BLOCK_MOVEMENT_NORTH),
    BLOCK_MOVEMENT_NORTH_EAST(CollisionDataFlag.BLOCK_MOVEMENT_NORTH_EAST),
    BLOCK_MOVEMENT_EAST(CollisionDataFlag.BLOCK_MOVEMENT_EAST),
    BLOCK_MOVEMENT_SOUTH_EAST(CollisionDataFlag.BLOCK_MOVEMENT_SOUTH_EAST),
    BLOCK_MOVEMENT_SOUTH(CollisionDataFlag.BLOCK_MOVEMENT_SOUTH),
    BLOCK_MOVEMENT_SOUTH_WEST(CollisionDataFlag.BLOCK_MOVEMENT_SOUTH_WEST),
    BLOCK_MOVEMENT_WEST(CollisionDataFlag.BLOCK_MOVEMENT_WEST),

    BLOCK_MOVEMENT_OBJECT(CollisionDataFlag.BLOCK_MOVEMENT_OBJECT),
    BLOCK_MOVEMENT_FLOOR_DECORATION(CollisionDataFlag.BLOCK_MOVEMENT_FLOOR_DECORATION),
    BLOCK_MOVEMENT_FLOOR(CollisionDataFlag.BLOCK_MOVEMENT_FLOOR),
    BLOCK_MOVEMENT_FULL(CollisionDataFlag.BLOCK_MOVEMENT_FULL);

    @Getter private int flag;

    /**
     * Returns the set of movement flags present in the given collision data.
     *
     * @param collisionData The tile collision flags.
     * @return The set of {@link MovementFlag}s that have been set.
     */
    public static Set<MovementFlag> getSetFlags(int collisionData) {
      return Arrays.stream(values())
          .filter(movementFlag -> (movementFlag.flag & collisionData) != 0)
          .collect(Collectors.toSet());
    }
  }
}
