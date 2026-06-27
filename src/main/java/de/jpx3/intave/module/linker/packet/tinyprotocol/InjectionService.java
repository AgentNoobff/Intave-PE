package de.jpx3.intave.module.linker.packet.tinyprotocol;

import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import de.jpx3.intave.IntavePlugin;
import de.jpx3.intave.check.EventProcessor;
import de.jpx3.intave.module.linker.packet.FilteringPacketAdapter;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Packet injection service.
 *
 * <p>Historically this used TinyProtocol to inject a Netty {@code ChannelDuplexHandler} into every
 * player's pipeline so that raw NMS packets could be dispatched to
 * {@link FilteringPacketAdapter} subscribers.  After the ProtocolLib → PacketEvents migration all
 * packet routing is handled by PacketEvents' own pipeline (via
 * {@link de.jpx3.intave.module.linker.packet.FilteringPacketAdapter} registered through
 * {@code PacketEvents.getAPI().getEventManager()}).  Injecting an additional Netty handler in
 * front of {@code packetevents_decoder} corrupts the {@code ByteBuf} reference count
 * ({@code refCnt: 0, decrement: 1}) and kicks every player that joins.
 *
 * <p>The TinyProtocol pipeline injection is therefore fully disabled.  This class is kept as a
 * thin facade that still manages the {@link FilteringPacketAdapter} subscription map used by
 * {@link de.jpx3.intave.module.linker.packet.PacketSubscriptionLinker#refreshLinkages()}.
 */
public final class InjectionService implements EventProcessor {
  private final Map<PacketTypeCommon, Collection<FilteringPacketAdapter>> packetListeners = new ConcurrentHashMap<>();
  // Retained for API compatibility — no Netty injection is performed.
  @SuppressWarnings("unused")
  private final IntavePlugin plugin;

  public InjectionService(IntavePlugin plugin) {
    this.plugin = plugin;
    // No pipeline injection: PacketEvents manages the Netty channel lifecycle.
  }

  Collection<FilteringPacketAdapter> subscriptionsOf(PacketTypeCommon type) {
    return packetListeners.get(type);
  }

  public void setupSubscriptions(PacketTypeCommon type, Collection<FilteringPacketAdapter> listeners) {
    packetListeners.put(type, listeners);
  }

  public void reset() {
    packetListeners.clear();
  }

  /** No-op: PacketEvents handles player channel injection automatically. */
  public void injectAll() {}

  /** No-op: PacketEvents handles player channel injection automatically. */
  public void inject(Player player) {}

  /** No-op: PacketEvents handles player channel uninject automatically. */
  public void uninjectAll() {}

  /** No-op: PacketEvents handles player channel uninject automatically. */
  public void uninject(Player player) {}
}
