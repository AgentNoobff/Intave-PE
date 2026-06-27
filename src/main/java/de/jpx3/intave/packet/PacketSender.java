package de.jpx3.intave.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import org.bukkit.entity.Player;

/**
 * Single entry point for sending packets to, and simulating packets from, players.
 *
 * <p>Backed by PacketEvents' {@link PlayerManager}. The {@code *Silently} variants bypass Intave's
 * (and other listeners') outbound/inbound processing, mirroring ProtocolLib's
 * {@code sendServerPacket(.., false)} and the historical receive-without-event behaviour.
 */
public final class PacketSender {
  private PacketSender() {
  }

  private static PlayerManager playerManager() {
    return PacketEvents.getAPI().getPlayerManager();
  }

  public static void sendServerPacket(Player receiver, PacketWrapper<?> packet) {
    playerManager().sendPacket(receiver, packet);
  }

  public static void sendServerPacketWithoutEvent(Player receiver, PacketWrapper<?> packet) {
    playerManager().sendPacketSilently(receiver, packet);
  }

  public static void receiveClientPacketFrom(Player receiver, PacketWrapper<?> packet) {
    playerManager().receivePacket(receiver, packet);
  }

  public static void receiveClientPacketSilentlyFrom(Player receiver, PacketWrapper<?> packet) {
    playerManager().receivePacketSilently(receiver, packet);
  }
}
