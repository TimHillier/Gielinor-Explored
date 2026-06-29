package com.gielinorexplored;

import java.awt.*;
import net.runelite.client.config.*;

/** Plugin Config. */
@ConfigGroup("gielinorExplored")
public interface gielinorExploredConfig extends Config {

  /** Turn Fog on or off */
  @ConfigItem(keyName = "fogshow", name = "Show Fog", description = "Show Fog")
  default boolean enableFog() {
    return true;
  }

  /** Toggles to show Fog on map */
  @ConfigItem(
      keyName = "mapshow",
      name = "Show Fog on World Map",
      description = "Show Fog on World Map")
  default boolean showOnWorldMap() {
    return true;
  }

  /** Toggles to show Fog on minimap */
  @ConfigItem(
      keyName = "minimapshow",
      name = "Show Fog on Mini Map",
      description = "Show Fog on Mini Map")
  default boolean showOnMiniMap() {
    return true;
  }

  /** Option for Fog Color */
  @Alpha
  @ConfigItem(
      keyName = "fogColor",
      name = "Fog Color",
      description = "Change the color of the fog.")
  default Color fogColor() {
    return new Color(0, 0, 0, 200);
  }
}
