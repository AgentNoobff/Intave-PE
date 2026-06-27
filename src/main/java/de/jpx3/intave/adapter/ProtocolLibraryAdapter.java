package de.jpx3.intave.adapter;

import com.github.retrooper.packetevents.PacketEvents;
import de.jpx3.intave.IntaveLogger;
import de.jpx3.intave.access.InvalidDependencyException;
import org.bukkit.Bukkit;

/**
 * Compatibility/availability checks for the packet backend.
 *
 * <p>Historically this validated ProtocolLib; the plugin now runs on PacketEvents,
 * which ships a single artifact covering Minecraft 1.8 through the latest releases
 * and exposes a stable API surface, so the elaborate per-method reflection probing
 * that ProtocolLib required is no longer necessary.
 */
public final class ProtocolLibraryAdapter {
  @Deprecated
  public static MinecraftVersion serverVersion() {
    return MinecraftVersion.current();
  }

  public static boolean packetEventsAvailable() {
    return Bukkit.getPluginManager().getPlugin("packetevents") != null
        && PacketEvents.getAPI() != null;
  }

  public static void checkIfOutdated() {
    if (!packetEventsAvailable()) {
      throw new InvalidDependencyException("PacketEvents is not available");
    }
    if (!PacketEvents.getAPI().isInitialized()) {
      IntaveLogger.logger().info("PacketEvents is present but not yet initialized");
    }
  }
}
